#!/usr/bin/env node

/**
 * Read-only audit: finds cloud data where a tied game (equal team scores) still has
 * player_game rows stored as Lose — the getResultForTeam(Tie) bug introduced in git
 * 77904d7 (2026-01-14, "refactor active game division schema"); tie handling was missing
 * until a later fix.
 *
 * Does NOT modify Firebase.
 *
 * Usage:
 *   node check-tie-result-bug.js
 *   node check-tie-result-bug.js --user <userId>
 */

const admin = require('firebase-admin');
const path = require('path');

const FIREBASE_CONFIG = {
    databaseURL: 'https://teampickerv1.firebaseio.com',
    projectId: 'teampickerv1'
};

/** ResultEnum.Lose stored value in SQLite; cloud may use string "Lose" */
const LOSE_VALUE = -1;

const SKIP_ROOT_KEYS = new Set(['configurations']);

function parseArgs() {
    const args = process.argv.slice(2);
    const options = { user: null };
    for (let i = 0; i < args.length; i++) {
        if (args[i] === '--user') options.user = args[++i];
        else if (args[i] === '--help') {
            printUsage();
            process.exit(0);
        }
    }
    return options;
}

function printUsage() {
    console.log(`
check-tie-result-bug.js — Audit tie-game / Lose mismatch (read-only)

Detects: games where team1Score === team2Score but a Team1/Team2 player row
         still has result = Lose (buggy); should be Tie.

Usage:
  node check-tie-result-bug.js              # all top-level users
  node check-tie-result-bug.js --user <uid> # one user

Setup: npm install in scripts/, serviceAccountKey.json in scripts/

See scripts/README.md
`);
}

async function initializeFirebase() {
    const serviceAccountPath =
        process.env.GOOGLE_APPLICATION_CREDENTIALS ||
        path.join(__dirname, 'serviceAccountKey.json');
    let serviceAccount;
    try {
        serviceAccount = require(serviceAccountPath);
    } catch (e) {
        console.error('❌ Could not load serviceAccountKey.json');
        console.error('   Save Firebase service account JSON as scripts/serviceAccountKey.json');
        process.exit(1);
    }
    admin.initializeApp({
        credential: admin.credential.cert(serviceAccount),
        databaseURL: FIREBASE_CONFIG.databaseURL
    });
    console.log(`✓ Connected to ${FIREBASE_CONFIG.projectId}\n`);
    return admin.database();
}

function isLoseResult(result) {
    if (result === undefined || result === null) return false;
    if (typeof result === 'number') return result === LOSE_VALUE;
    if (typeof result === 'string') return result === 'Lose';
    return false;
}

/** Team1 / Team2 field players only (not Bench / Missed semantics) */
function isFieldTeam(team) {
    if (team === undefined || team === null) return false;
    if (typeof team === 'number') return team === 0 || team === 1;
    if (typeof team === 'string') {
        return team === 'Team1' || team === 'Team2' || team === '0' || team === '1';
    }
    return false;
}

/**
 * @returns {{ tieGameIds: Set<string>, tieGameCount: number }}
 */
function buildTieGameIndex(gamesVal) {
    const tieGameIds = new Set();
    if (!gamesVal || typeof gamesVal !== 'object') {
        return { tieGameIds, tieGameCount: 0 };
    }
    for (const [gameId, g] of Object.entries(gamesVal)) {
        if (!g || typeof g !== 'object') continue;
        const s1 = g.team1Score ?? g.team_one_score;
        const s2 = g.team2Score ?? g.team_two_score;
        if (s1 === undefined || s2 === undefined) continue;
        if (Number(s1) === Number(s2)) {
            tieGameIds.add(String(gameId));
        }
    }
    return { tieGameIds, tieGameCount: tieGameIds.size };
}

/** Latest app date string among tie games that still have buggy Lose rows (see /games dateString). */
function getGameDateFromVal(gamesVal, gameIdStr) {
    if (!gamesVal || typeof gamesVal !== 'object') return null;
    const g = gamesVal[gameIdStr];
    if (!g || typeof g !== 'object') return null;
    const d = g.dateString ?? g.date_string;
    if (d === undefined || d === null || d === '') return null;
    return String(d);
}

function maxDateString(a, b) {
    if (!a) return b;
    if (!b) return a;
    const ta = Date.parse(a);
    const tb = Date.parse(b);
    if (!Number.isNaN(ta) && !Number.isNaN(tb)) {
        return ta >= tb ? a : b;
    }
    return a >= b ? a : b;
}

/**
 * Count buggy rows and affected players for one user.
 */
function analyzeUserData(userId, gamesVal, playersGamesVal) {
    const { tieGameIds } = buildTieGameIndex(gamesVal);
    let buggyRows = 0;
    const affectedPlayers = new Set();
    const buggyGameIds = new Set();

    if (!playersGamesVal || typeof playersGamesVal !== 'object') {
        return {
            tieGames: tieGameIds.size,
            buggyRows,
            affectedPlayers: 0,
            affectedPlayerNames: [],
            maxAffectedGameDate: null
        };
    }

    for (const [playerName, byGame] of Object.entries(playersGamesVal)) {
        if (!byGame || typeof byGame !== 'object') continue;
        for (const [gameId, row] of Object.entries(byGame)) {
            if (!row || typeof row !== 'object') continue;
            const gid = String(gameId);
            if (!tieGameIds.has(gid)) continue;
            if (!isFieldTeam(row.team)) continue;
            if (isLoseResult(row.result)) {
                buggyRows++;
                affectedPlayers.add(playerName);
                buggyGameIds.add(gid);
            }
        }
    }

    let maxAffectedGameDate = null;
    for (const gid of buggyGameIds) {
        const ds = getGameDateFromVal(gamesVal, gid);
        if (ds) maxAffectedGameDate = maxDateString(maxAffectedGameDate, ds);
    }

    return {
        tieGames: tieGameIds.size,
        buggyRows,
        affectedPlayers: affectedPlayers.size,
        affectedPlayerNames: [...affectedPlayers].sort(),
        maxAffectedGameDate
    };
}

async function analyzeUser(db, userId) {
    const gamesSnap = await db.ref(`${userId}/games`).once('value');
    const pgSnap = await db.ref(`${userId}/playersGames`).once('value');
    const accountSnap = await db.ref(`${userId}/account`).once('value');
    const account = accountSnap.val() || {};
    const gamesVal = gamesSnap.val();
    const pgVal = pgSnap.val();
    return {
        userId,
        email: account.email || '—',
        displayName: account.displayName || '—',
        ...analyzeUserData(userId, gamesVal, pgVal)
    };
}

function printRow(r) {
    const email = String(r.email).substring(0, 36).padEnd(36);
    const name = String(r.displayName).substring(0, 18).padEnd(18);
    const tg = String(r.tieGames).padStart(5);
    const rows = String(r.buggyRows).padStart(6);
    const pl = String(r.affectedPlayers).padStart(5);
    const maxD = r.maxAffectedGameDate
        ? String(r.maxAffectedGameDate).substring(0, 14).padEnd(14)
        : '—'.padEnd(14);
    const flag = r.buggyRows > 0 ? 'YES' : 'no';
    console.log(
        ` ${email} │ ${name} │ ${tg} │ ${rows} │ ${pl} │ ${maxD} │ ${flag}`
    );
}

async function main() {
    const options = parseArgs();
    const db = await initializeFirebase();

    if (options.user) {
        const snap = await db.ref(options.user).once('value');
        if (!snap.exists()) {
            console.error(`❌ User "${options.user}" not found.`);
            process.exit(1);
        }
        const r = await analyzeUser(db, options.user);
        console.log('══════════════════════════════════════════════════════════════════');
        console.log(' Single user audit');
        console.log('══════════════════════════════════════════════════════════════════');
        console.log(` User ID:     ${r.userId}`);
        console.log(` Email:       ${r.email}`);
        console.log(` Display:     ${r.displayName}`);
        console.log(` Tie games:   ${r.tieGames} (equal scores in /games)`);
        console.log(` Buggy rows:  ${r.buggyRows} (Team1/2 + Lose on a tie game)`);
        console.log(` Players hit: ${r.affectedPlayers}`);
        console.log(
            ` Max date:    ${r.maxAffectedGameDate ?? '—'} (latest games.dateString among games with buggy rows)`
        );
        if (r.buggyRows > 0 && r.affectedPlayerNames && r.affectedPlayerNames.length) {
            console.log('\n Affected player names (sample):');
            const sample = r.affectedPlayerNames.slice(0, 30);
            sample.forEach((n) => console.log(`   - ${n}`));
            if (r.affectedPlayerNames.length > 30) {
                console.log(`   ... and ${r.affectedPlayerNames.length - 30} more`);
            }
        }
        console.log('══════════════════════════════════════════════════════════════════');
        process.exit(0);
    }

    console.log('📖 Loading root...');
    const rootSnap = await db.ref('/').once('value');
    const data = rootSnap.val() || {};
    const keys = Object.keys(data).filter((k) => !SKIP_ROOT_KEYS.has(k));

    console.log(`   ${keys.length} top-level keys (excluding known non-user roots)\n`);

    const results = [];
    for (const userId of keys) {
        const node = data[userId];
        if (!node || typeof node !== 'object') continue;
        if (!node.games && !node.playersGames) continue;

        try {
            const r = await analyzeUser(db, userId);
            results.push(r);
        } catch (e) {
            console.error(`   ⚠️  ${userId}: ${e.message}`);
        }
    }

    results.sort((a, b) => b.buggyRows - a.buggyRows);

    console.log('══════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════');
    console.log(' Email                                │ Display Name       │ Ties │ BugRows │ Plrs │ Max bug date   │ Evident?');
    console.log('──────────────────────────────────────┼────────────────────┼──────┼─────────┼──────┼────────────────┼─────────');

    let totalBuggy = 0;
    let usersWithBug = 0;

    for (const r of results) {
        printRow(r);
        totalBuggy += r.buggyRows;
        if (r.buggyRows > 0) usersWithBug++;
    }

    console.log('══════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════');
    console.log(
        `\nSummary: ${results.length} user(s) scanned, ${usersWithBug} with evident bug, ${totalBuggy} total buggy row(s).\n`
    );
    console.log(
        'Columns: Ties = games with team1Score = team2Score; BugRows = Team1/2 rows with result Lose on those games;'
    );
    console.log(
        'Max bug date = max games.dateString over games that have ≥1 buggy row (tie + Lose). Read-only — no data was changed.\n'
    );

    process.exit(0);
}

main().catch((err) => {
    console.error(err);
    process.exit(1);
});

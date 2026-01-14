#!/usr/bin/env node

/**
 * Script to copy, compare, or check games in Firebase Realtime Database.
 * 
 * Copy mode (default): Copies games from one user to another.
 *   Only inserts new records - never overwrites existing data.
 * 
 * Compare mode: Shows differences between two users' games.
 * 
 * Check-orphans mode: Finds games without player_games records (orphaned games).
 * 
 * Usage:
 *   Copy:          node copy-games.js --from <userId> --to <userId> --start <id> --end <id> [--dry-run]
 *   Compare:       node copy-games.js --compare --from <userId> --to <userId>
 *   Check orphans: node copy-games.js --check-orphans --user <userId>
 * 
 * Setup:
 *   1. Download your Firebase service account key from Firebase Console:
 *      Project Settings > Service Accounts > Generate New Private Key
 *   2. Save it as 'serviceAccountKey.json' in the same directory as this script
 */

const admin = require('firebase-admin');
const path = require('path');

// Firebase configuration from google-services.json
const FIREBASE_CONFIG = {
    databaseURL: 'https://teampickerv1.firebaseio.com',
    projectId: 'teampickerv1'
};

// Parse command line arguments
function parseArgs() {
    const args = process.argv.slice(2);
    const options = {
        from: null,
        to: null,
        user: null,
        start: null,
        end: null,
        dryRun: false,
        compare: false,
        checkOrphans: false
    };

    for (let i = 0; i < args.length; i++) {
        switch (args[i]) {
            case '--from':
                options.from = args[++i];
                break;
            case '--to':
                options.to = args[++i];
                break;
            case '--user':
                options.user = args[++i];
                break;
            case '--start':
                options.start = parseInt(args[++i], 10);
                break;
            case '--end':
                options.end = parseInt(args[++i], 10);
                break;
            case '--dry-run':
                options.dryRun = true;
                break;
            case '--compare':
                options.compare = true;
                break;
            case '--check-orphans':
                options.checkOrphans = true;
                break;
            case '--help':
                printUsage();
                process.exit(0);
        }
    }

    return options;
}

function printUsage() {
    console.log(`
Copy/Compare/Check Games Script

COPY MODE (default):
  Copies games from one user to another (INSERT ONLY, never overwrites)

  Usage: node copy-games.js --from <userId> --to <userId> --start <id> --end <id> [--dry-run]

  Required:
    --from <userId>     Source user UID (copy FROM this user)
    --to <userId>       Target user UID (copy TO this user)
    --start <gameId>    Start game ID (inclusive)
    --end <gameId>      End game ID (inclusive)

  Options:
    --dry-run           Preview changes without writing to database

COMPARE MODE:
  Shows differences between two users' games

  Usage: node copy-games.js --compare --from <userId> --to <userId>

  Required:
    --compare           Enable compare mode
    --from <userId>     First user UID
    --to <userId>       Second user UID

CHECK ORPHANS MODE:
  Finds games that have no player_games records (orphaned games)

  Usage: node copy-games.js --check-orphans --user <userId>

  Required:
    --check-orphans     Enable check orphans mode
    --user <userId>     User UID to check

General:
  --help              Show this help message

Examples:
  # Find orphaned games (games without player_games records)
  node copy-games.js --check-orphans --user "userA_uid"

  # Compare games between two users
  node copy-games.js --compare --from "userA_uid" --to "userB_uid"

  # Preview what would be copied
  node copy-games.js --from "userB_uid" --to "userA_uid" --start 1 --end 300 --dry-run

  # Copy games 1-300 from userB to userA
  node copy-games.js --from "userB_uid" --to "userA_uid" --start 1 --end 300
`);
}

function validateOptions(options) {
    const errors = [];
    
    if (options.checkOrphans) {
        // Check orphans mode
        if (!options.user) errors.push('--user (user ID) is required for --check-orphans');
    } else if (options.compare) {
        // Compare mode
        if (!options.from) errors.push('--from (user ID) is required');
        if (!options.to) errors.push('--to (user ID) is required');
        if (options.from === options.to) errors.push('User IDs must be different');
    } else {
        // Copy mode
        if (!options.from) errors.push('--from (user ID) is required');
        if (!options.to) errors.push('--to (user ID) is required');
        if (options.from === options.to) errors.push('User IDs must be different');
        if (options.start === null || isNaN(options.start)) errors.push('--start (start game ID) is required and must be a number');
        if (options.end === null || isNaN(options.end)) errors.push('--end (end game ID) is required and must be a number');
        if (options.start !== null && options.end !== null && options.start > options.end) {
            errors.push('--start must be less than or equal to --end');
        }
    }

    if (errors.length > 0) {
        console.error('Error(s):');
        errors.forEach(e => console.error(`  - ${e}`));
        console.error('\nRun with --help for usage information.');
        process.exit(1);
    }
}

async function initializeFirebase() {
    try {
        // Try to load service account key from file
        const serviceAccountPath = process.env.GOOGLE_APPLICATION_CREDENTIALS || 
            path.join(__dirname, 'serviceAccountKey.json');
        
        let serviceAccount;
        try {
            serviceAccount = require(serviceAccountPath);
        } catch (e) {
            console.error('❌ Could not find serviceAccountKey.json');
            console.error('\nTo get your service account key:');
            console.error('1. Go to Firebase Console: https://console.firebase.google.com');
            console.error('2. Select project "teampickerv1"');
            console.error('3. Go to Project Settings > Service Accounts');
            console.error('4. Click "Generate New Private Key"');
            console.error('5. Save the file as "serviceAccountKey.json" in the scripts folder');
            process.exit(1);
        }
        
        admin.initializeApp({
            credential: admin.credential.cert(serviceAccount),
            databaseURL: FIREBASE_CONFIG.databaseURL
        });
        
        console.log(`✓ Connected to Firebase: ${FIREBASE_CONFIG.projectId}`);
        return admin.database();
    } catch (error) {
        console.error('Failed to initialize Firebase:', error.message);
        process.exit(1);
    }
}

async function copyGames(db, options) {
    const { from, to, start, end, dryRun } = options;
    
    console.log('\n════════════════════════════════════════════');
    console.log('           COPY GAMES (INSERT ONLY)          ');
    console.log('════════════════════════════════════════════');
    console.log(`From user:   ${from}`);
    console.log(`To user:     ${to}`);
    console.log(`Game range:  ${start} to ${end}`);
    console.log(`Mode:        ${dryRun ? '🔍 DRY RUN (no changes)' : '⚡ LIVE'}`);
    console.log('────────────────────────────────────────────\n');

    // Step 1: Verify source user exists
    console.log('📖 Checking source user...');
    const sourceRef = db.ref(from);
    const sourceSnapshot = await sourceRef.once('value');
    if (!sourceSnapshot.exists()) {
        console.error(`❌ Source user "${from}" does not exist!`);
        process.exit(1);
    }
    console.log('   ✓ Source user found');

    // Step 2: Fetch source games
    console.log('\n📖 Fetching source games...');
    const sourceGamesRef = db.ref(`${from}/games`);
    const sourceGamesSnapshot = await sourceGamesRef.once('value');
    const sourceGames = sourceGamesSnapshot.val() || {};
    
    // Filter games in range
    const gamesToCopy = {};
    
    for (const [gameId, gameData] of Object.entries(sourceGames)) {
        const id = parseInt(gameId, 10);
        if (id >= start && id <= end) {
            gamesToCopy[gameId] = gameData;
        }
    }
    
    console.log(`   Found ${Object.keys(gamesToCopy).length} games in range ${start}-${end}`);
    
    if (Object.keys(gamesToCopy).length === 0) {
        console.log('\n⚠️  No games found to copy in the specified range.');
        return;
    }

    // Step 3: Check target user and existing games
    console.log('\n📖 Checking target user...');
    const targetRef = db.ref(to);
    const targetSnapshot = await targetRef.once('value');
    
    if (!targetSnapshot.exists()) {
        console.log(`   Target user "${to}" does not exist - will be created`);
    } else {
        console.log('   ✓ Target user exists');
    }

    const targetGamesRef = db.ref(`${to}/games`);
    const targetGamesSnapshot = await targetGamesRef.once('value');
    const targetGames = targetGamesSnapshot.val() || {};
    
    // Filter out games that already exist in target (INSERT ONLY - never overwrite)
    const gamesToInsert = {};
    const skippedGames = [];
    
    for (const [gameId, gameData] of Object.entries(gamesToCopy)) {
        if (targetGames[gameId]) {
            skippedGames.push(gameId);
        } else {
            gamesToInsert[gameId] = gameData;
        }
    }
    
    if (skippedGames.length > 0) {
        console.log(`   ⏭️  Skipping ${skippedGames.length} games that already exist in target`);
    }

    // Step 4: Fetch playersGames for the games we're inserting
    console.log('\n📖 Fetching source player-game records...');
    const sourcePlayersGamesRef = db.ref(`${from}/playersGames`);
    const sourcePlayersGamesSnapshot = await sourcePlayersGamesRef.once('value');
    const sourcePlayersGames = sourcePlayersGamesSnapshot.val() || {};
    
    // Also check target's existing player-game records
    const targetPlayersGamesRef = db.ref(`${to}/playersGames`);
    const targetPlayersGamesSnapshot = await targetPlayersGamesRef.once('value');
    const targetPlayersGames = targetPlayersGamesSnapshot.val() || {};
    
    // Build a map of playerGames to insert (only for games we're copying)
    const playerGamesToInsert = {};
    let playerGamesTotal = 0;
    let playerGamesSkipped = 0;
    
    const gameIdsToInsert = new Set(Object.keys(gamesToInsert));
    
    for (const [playerName, playerGames] of Object.entries(sourcePlayersGames)) {
        if (!playerGames) continue;
        
        for (const [gameId, gameData] of Object.entries(playerGames)) {
            if (gameIdsToInsert.has(gameId)) {
                // Check if this player-game record already exists in target
                if (targetPlayersGames[playerName]?.[gameId]) {
                    playerGamesSkipped++;
                    continue;
                }
                
                if (!playerGamesToInsert[playerName]) {
                    playerGamesToInsert[playerName] = {};
                }
                playerGamesToInsert[playerName][gameId] = gameData;
                playerGamesTotal++;
            }
        }
    }
    
    console.log(`   Found ${playerGamesTotal} player-game records to insert`);
    if (playerGamesSkipped > 0) {
        console.log(`   ⏭️  Skipping ${playerGamesSkipped} player-game records that already exist`);
    }

    // Step 5: Display summary
    console.log('\n════════════════════════════════════════════');
    console.log('                  SUMMARY                    ');
    console.log('════════════════════════════════════════════');
    console.log(`Games to INSERT:              ${Object.keys(gamesToInsert).length}`);
    console.log(`Games SKIPPED (exist):        ${skippedGames.length}`);
    console.log(`Player-game records to INSERT: ${playerGamesTotal}`);
    console.log(`Player-game records SKIPPED:   ${playerGamesSkipped}`);
    
    if (Object.keys(gamesToInsert).length > 0) {
        console.log('\n📋 Games to be inserted:');
        const sortedGameIds = Object.keys(gamesToInsert).sort((a, b) => parseInt(a) - parseInt(b));
        for (const gameId of sortedGameIds) {
            const game = gamesToInsert[gameId];
            const date = game.dateString || 'unknown date';
            const score = `${game.team1Score ?? '?'}-${game.team2Score ?? '?'}`;
            console.log(`   Game ${gameId.padStart(4)}: ${date} (${score})`);
        }
    }

    if (Object.keys(gamesToInsert).length === 0) {
        console.log('\n✅ Nothing to insert - all games already exist in target.');
        return;
    }

    // Step 6: Perform the insert
    if (dryRun) {
        console.log('\n════════════════════════════════════════════');
        console.log('        🔍 DRY RUN - NO CHANGES MADE         ');
        console.log('════════════════════════════════════════════');
        console.log('Run without --dry-run to actually insert the data.\n');
        return;
    }

    console.log('\n════════════════════════════════════════════');
    console.log('              INSERTING DATA                 ');
    console.log('════════════════════════════════════════════');
    
    // Insert games
    console.log('\n📝 Inserting games...');
    let gamesInserted = 0;
    for (const [gameId, gameData] of Object.entries(gamesToInsert)) {
        await db.ref(`${to}/games/${gameId}`).set(gameData);
        gamesInserted++;
        if (gamesInserted % 10 === 0 || gamesInserted === Object.keys(gamesToInsert).length) {
            console.log(`   Inserted ${gamesInserted}/${Object.keys(gamesToInsert).length} games`);
        }
    }
    
    // Insert player games
    console.log('\n📝 Inserting player-game records...');
    let playerGamesInserted = 0;
    for (const [playerName, playerGames] of Object.entries(playerGamesToInsert)) {
        for (const [gameId, gameData] of Object.entries(playerGames)) {
            await db.ref(`${to}/playersGames/${playerName}/${gameId}`).set(gameData);
            playerGamesInserted++;
        }
    }
    console.log(`   Inserted ${playerGamesInserted} player-game records`);

    console.log('\n════════════════════════════════════════════');
    console.log('               ✅ COMPLETE                   ');
    console.log('════════════════════════════════════════════');
    console.log(`Games inserted:        ${gamesInserted}`);
    console.log(`Player-games inserted: ${playerGamesInserted}`);
    console.log('');
}

async function checkOrphanedGames(db, options) {
    const { user } = options;
    
    console.log('\n════════════════════════════════════════════');
    console.log('           CHECK ORPHANED GAMES              ');
    console.log('════════════════════════════════════════════');
    console.log(`User: ${user}`);
    console.log('────────────────────────────────────────────\n');

    // Fetch games
    console.log('📖 Fetching games...');
    const gamesRef = db.ref(`${user}/games`);
    const gamesSnapshot = await gamesRef.once('value');
    const games = gamesSnapshot.val() || {};
    const gameIds = new Set(Object.keys(games));
    console.log(`   Found ${gameIds.size} games`);

    // Fetch playersGames and extract unique game IDs
    console.log('\n📖 Fetching player-games...');
    const playersGamesRef = db.ref(`${user}/playersGames`);
    const playersGamesSnapshot = await playersGamesRef.once('value');
    const playersGames = playersGamesSnapshot.val() || {};
    
    const gameIdsWithPlayers = new Set();
    let totalPlayerGameRecords = 0;
    
    for (const [playerName, playerGames] of Object.entries(playersGames)) {
        if (!playerGames) continue;
        for (const gameId of Object.keys(playerGames)) {
            gameIdsWithPlayers.add(gameId);
            totalPlayerGameRecords++;
        }
    }
    
    console.log(`   Found ${totalPlayerGameRecords} player-game records`);
    console.log(`   Covering ${gameIdsWithPlayers.size} unique games`);

    // Find orphaned games (in games but not in playersGames)
    const orphanedGames = [];
    const gamesWithPlayers = [];
    
    for (const gameId of gameIds) {
        const game = games[gameId];
        const entry = {
            gameId: parseInt(gameId, 10),
            date: game.dateString || 'unknown'
        };
        
        if (gameIdsWithPlayers.has(gameId)) {
            gamesWithPlayers.push(entry);
        } else {
            orphanedGames.push(entry);
        }
    }
    
    // Also check for player-games that reference non-existent games
    const orphanedPlayerGames = [];
    for (const gameId of gameIdsWithPlayers) {
        if (!gameIds.has(gameId)) {
            orphanedPlayerGames.push(parseInt(gameId, 10));
        }
    }
    
    // Sort by date ascending
    const sortByDate = (a, b) => {
        if (a.date === 'unknown') return 1;
        if (b.date === 'unknown') return -1;
        return a.date.localeCompare(b.date);
    };
    
    orphanedGames.sort(sortByDate);

    // Display results
    console.log('\n════════════════════════════════════════════');
    console.log('                 SUMMARY                     ');
    console.log('════════════════════════════════════════════');
    console.log(`Total games:                    ${gameIds.size}`);
    console.log(`Games WITH player records:      ${gamesWithPlayers.length}`);
    console.log(`Games WITHOUT player records:   ${orphanedGames.length} ⚠️`);
    if (orphanedPlayerGames.length > 0) {
        console.log(`Player-games for missing games: ${orphanedPlayerGames.length} ⚠️`);
    }
    
    if (orphanedGames.length > 0) {
        console.log('\n────────────────────────────────────────────');
        console.log('📋 ORPHANED GAMES (no player-game records):');
        console.log('   Game ID  │  Date');
        console.log('   ─────────┼────────────');
        for (const game of orphanedGames) {
            console.log(`   ${String(game.gameId).padStart(7)} │  ${game.date}`);
        }
        
        // Print game IDs as a comma-separated list for easy copying
        console.log('\n   Game IDs (comma-separated):');
        console.log(`   ${orphanedGames.map(g => g.gameId).join(', ')}`);
    } else {
        console.log('\n✅ No orphaned games found! All games have player records.');
    }
    
    if (orphanedPlayerGames.length > 0) {
        console.log('\n────────────────────────────────────────────');
        console.log('⚠️  ORPHANED PLAYER-GAMES (game record missing):');
        orphanedPlayerGames.sort((a, b) => a - b);
        console.log(`   Game IDs: ${orphanedPlayerGames.join(', ')}`);
    }
    
    console.log('');
}

async function compareGames(db, options) {
    const { from, to } = options;
    
    console.log('\n════════════════════════════════════════════');
    console.log('              COMPARE GAMES                  ');
    console.log('════════════════════════════════════════════');
    console.log(`User A: ${from}`);
    console.log(`User B: ${to}`);
    console.log('────────────────────────────────────────────\n');

    // Fetch games from both users
    console.log('📖 Fetching games from both users...');
    
    const userAGamesRef = db.ref(`${from}/games`);
    const userBGamesRef = db.ref(`${to}/games`);
    
    const [userASnapshot, userBSnapshot] = await Promise.all([
        userAGamesRef.once('value'),
        userBGamesRef.once('value')
    ]);
    
    const userAGames = userASnapshot.val() || {};
    const userBGames = userBSnapshot.val() || {};
    
    const userAGameIds = new Set(Object.keys(userAGames));
    const userBGameIds = new Set(Object.keys(userBGames));
    
    console.log(`   User A has ${userAGameIds.size} games`);
    console.log(`   User B has ${userBGameIds.size} games`);

    // Find differences
    const onlyInA = [];
    const onlyInB = [];
    const inBoth = [];
    
    for (const gameId of userAGameIds) {
        const game = userAGames[gameId];
        const entry = {
            gameId: parseInt(gameId, 10),
            date: game.dateString || 'unknown'
        };
        if (userBGameIds.has(gameId)) {
            inBoth.push(entry);
        } else {
            onlyInA.push(entry);
        }
    }
    
    for (const gameId of userBGameIds) {
        if (!userAGameIds.has(gameId)) {
            const game = userBGames[gameId];
            onlyInB.push({
                gameId: parseInt(gameId, 10),
                date: game.dateString || 'unknown'
            });
        }
    }
    
    // Sort by date ascending
    const sortByDate = (a, b) => {
        if (a.date === 'unknown') return 1;
        if (b.date === 'unknown') return -1;
        return a.date.localeCompare(b.date);
    };
    
    onlyInA.sort(sortByDate);
    onlyInB.sort(sortByDate);
    inBoth.sort(sortByDate);

    // Display results
    console.log('\n════════════════════════════════════════════');
    console.log('                 SUMMARY                     ');
    console.log('════════════════════════════════════════════');
    console.log(`Games only in User A: ${onlyInA.length}`);
    console.log(`Games only in User B: ${onlyInB.length}`);
    console.log(`Games in both:        ${inBoth.length}`);
    
    if (onlyInA.length > 0) {
        console.log('\n────────────────────────────────────────────');
        console.log(`📋 Games ONLY in User A (${from}):`);
        console.log('   Game ID  │  Date');
        console.log('   ─────────┼────────────');
        for (const game of onlyInA) {
            console.log(`   ${String(game.gameId).padStart(7)} │  ${game.date}`);
        }
    }
    
    if (onlyInB.length > 0) {
        console.log('\n────────────────────────────────────────────');
        console.log(`📋 Games ONLY in User B (${to}):`);
        console.log('   Game ID  │  Date');
        console.log('   ─────────┼────────────');
        for (const game of onlyInB) {
            console.log(`   ${String(game.gameId).padStart(7)} │  ${game.date}`);
        }
    }
    
    if (inBoth.length > 0 && (onlyInA.length > 0 || onlyInB.length > 0)) {
        console.log('\n────────────────────────────────────────────');
        console.log(`✓ Games in BOTH users: ${inBoth.length}`);
        if (inBoth.length <= 20) {
            console.log('   Game ID  │  Date');
            console.log('   ─────────┼────────────');
            for (const game of inBoth) {
                console.log(`   ${String(game.gameId).padStart(7)} │  ${game.date}`);
            }
        } else {
            const first5 = inBoth.slice(0, 5);
            const last5 = inBoth.slice(-5);
            console.log('   Game ID  │  Date');
            console.log('   ─────────┼────────────');
            for (const game of first5) {
                console.log(`   ${String(game.gameId).padStart(7)} │  ${game.date}`);
            }
            console.log(`   ... (${inBoth.length - 10} more) ...`);
            for (const game of last5) {
                console.log(`   ${String(game.gameId).padStart(7)} │  ${game.date}`);
            }
        }
    }
    
    if (onlyInA.length === 0 && onlyInB.length === 0) {
        console.log('\n✅ Both users have identical games!');
    }
    
    console.log('');
}

async function main() {
    const options = parseArgs();
    validateOptions(options);
    
    const db = await initializeFirebase();
    
    try {
        if (options.checkOrphans) {
            await checkOrphanedGames(db, options);
        } else if (options.compare) {
            await compareGames(db, options);
        } else {
            await copyGames(db, options);
        }
    } catch (error) {
        console.error('\n❌ Error:', error.message);
        if (error.code) {
            console.error('   Code:', error.code);
        }
        process.exit(1);
    }
    
    process.exit(0);
}

main();

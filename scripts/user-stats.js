#!/usr/bin/env node

/**
 * Script to query Firebase for user statistics.
 * 
 * Displays for each user:
 * - Number of unique players
 * - Number of games
 * - Last game date
 * 
 * Usage:
 *   All users:      node user-stats.js
 *   Specific user:  node user-stats.js --user <userId>
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
        user: null
    };

    for (let i = 0; i < args.length; i++) {
        switch (args[i]) {
            case '--user':
                options.user = args[++i];
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
User Statistics Script

Shows statistics for all users or a specific user:
  - Email and display name
  - Number of unique players
  - Number of games
  - Last game date

Usage:
  node user-stats.js [--user <userId>]

Options:
  --user <userId>     Show stats for specific user only (optional)
  --help              Show this help message

Commands:
  # Show stats for all users (sorted by number of games)
  node user-stats.js

  # Or using npm
  npm run user-stats

  # Show stats for a specific user
  node user-stats.js --user "user123"

  # Or using npm
  npm run user-stats -- --user "user123"

Setup:
  Before running, ensure you have:
  1. Installed dependencies: npm install
  2. Service account key saved as serviceAccountKey.json

Examples:
  # View all users with their email, player count, game count, and last game
  node user-stats.js

  # Check stats for a specific user
  node user-stats.js --user "abc123def456xyz"
`);
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

async function getUserStats(db, userId) {
    // Fetch account data
    const accountRef = db.ref(`${userId}/account`);
    const accountSnapshot = await accountRef.once('value');
    const account = accountSnapshot.val() || {};
    
    // Fetch games
    const gamesRef = db.ref(`${userId}/games`);
    const gamesSnapshot = await gamesRef.once('value');
    const games = gamesSnapshot.val() || {};
    
    // Fetch playersGames to count unique players
    const playersGamesRef = db.ref(`${userId}/playersGames`);
    const playersGamesSnapshot = await playersGamesRef.once('value');
    const playersGames = playersGamesSnapshot.val() || {};
    
    // Count unique players
    const playerNames = new Set(Object.keys(playersGames));
    const numPlayers = playerNames.size;
    
    // Count games
    const numGames = Object.keys(games).length;
    
    // Find last game date
    let lastGameDate = null;
    let lastGameId = null;
    
    for (const [gameId, gameData] of Object.entries(games)) {
        const dateString = gameData.dateString;
        if (dateString) {
            if (!lastGameDate || dateString > lastGameDate) {
                lastGameDate = dateString;
                lastGameId = gameId;
            }
        }
    }
    
    return {
        userId,
        email: account.email || 'No email',
        displayName: account.displayName || 'No name',
        numPlayers,
        numGames,
        lastGameDate: lastGameDate || 'No games',
        lastGameId
    };
}

async function showAllUsersStats(db) {
    console.log('\n════════════════════════════════════════════');
    console.log('           USER STATISTICS                   ');
    console.log('════════════════════════════════════════════\n');
    
    console.log('📖 Fetching all users...');
    const rootRef = db.ref('/');
    const rootSnapshot = await rootRef.once('value');
    const data = rootSnapshot.val() || {};
    
    const userIds = Object.keys(data);
    console.log(`   Found ${userIds.length} users\n`);
    
    if (userIds.length === 0) {
        console.log('No users found in database.');
        return;
    }
    
    console.log('📊 Analyzing user data...\n');
    
    const allStats = [];
    
    for (const userId of userIds) {
        try {
            const stats = await getUserStats(db, userId);
            allStats.push(stats);
        } catch (error) {
            console.error(`   ⚠️  Error fetching stats for user ${userId}:`, error.message);
        }
    }
    
    // Sort by number of games descending
    allStats.sort((a, b) => b.numGames - a.numGames);
    
    // Display results
    console.log('═══════════════════════════════════════════════════════════════════════════════════════════════════════');
    console.log(' Email                                │ Display Name         │ Players │ Games │ Last Game Date');
    console.log('──────────────────────────────────────┼──────────────────────┼─────────┼───────┼──────────────────');
    
    let totalPlayers = 0;
    let totalGames = 0;
    
    for (const stats of allStats) {
        const emailDisplay = stats.email.substring(0, 36).padEnd(36);
        const nameDisplay = stats.displayName.substring(0, 20).padEnd(20);
        const playersDisplay = String(stats.numPlayers).padStart(7);
        const gamesDisplay = String(stats.numGames).padStart(5);
        const dateDisplay = stats.lastGameDate;
        
        console.log(` ${emailDisplay} │ ${nameDisplay} │ ${playersDisplay} │ ${gamesDisplay} │ ${dateDisplay}`);
        
        totalPlayers += stats.numPlayers;
        totalGames += stats.numGames;
    }
    
    console.log('═══════════════════════════════════════════════════════════════════════════════════════════════════════');
    console.log(`\nTotal: ${allStats.length} users, ${totalGames} games\n`);
}

async function showUserStats(db, userId) {
    console.log('\n════════════════════════════════════════════');
    console.log('           USER STATISTICS                   ');
    console.log('════════════════════════════════════════════');
    console.log(`User ID: ${userId}`);
    console.log('────────────────────────────────────────────\n');
    
    console.log('📖 Fetching user data...');
    
    // Check if user exists
    const userRef = db.ref(userId);
    const userSnapshot = await userRef.once('value');
    
    if (!userSnapshot.exists()) {
        console.error(`\n❌ User "${userId}" does not exist!`);
        process.exit(1);
    }
    
    const stats = await getUserStats(db, userId);
    
    console.log('\n════════════════════════════════════════════');
    console.log('                RESULTS                      ');
    console.log('════════════════════════════════════════════');
    console.log(`Email:              ${stats.email}`);
    console.log(`Display name:       ${stats.displayName}`);
    console.log(`Number of players:  ${stats.numPlayers}`);
    console.log(`Number of games:    ${stats.numGames}`);
    console.log(`Last game date:     ${stats.lastGameDate}`);
    if (stats.lastGameId) {
        console.log(`Last game ID:       ${stats.lastGameId}`);
    }
    console.log('');
}

async function main() {
    const options = parseArgs();
    
    const db = await initializeFirebase();
    
    try {
        if (options.user) {
            await showUserStats(db, options.user);
        } else {
            await showAllUsersStats(db);
        }
    } catch (error) {
        console.error('\n❌ Error:', error.message);
        if (error.code) {
            console.error('   Code:', error.code);
        }
        if (error.stack) {
            console.error('\n', error.stack);
        }
        process.exit(1);
    }
    
    process.exit(0);
}

main();


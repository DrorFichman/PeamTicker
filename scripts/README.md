# TeamPicker Admin Scripts

Admin scripts for managing TeamPicker Firebase data.

## Setup

1. **Install dependencies:**
   ```bash
   cd scripts
   npm install
   ```

2. **Get Firebase service account key:**
   - Go to [Firebase Console](https://console.firebase.google.com/project/teampickerv1)
   - Go to Project Settings (gear icon) > Service Accounts
   - Click "Generate New Private Key"
   - Save the downloaded file as `serviceAccountKey.json` in this `scripts` folder

   ⚠️ **Keep this file secure!** It has full admin access to your Firebase project.
   Add it to .gitignore to prevent accidental commits:
   ```
   scripts/serviceAccountKey.json
   ```

## user-stats.js

Query Firebase to view statistics for all users or a specific user.

### Usage

```bash
# Show stats for all users
node user-stats.js

# Show stats for a specific user
node user-stats.js --user <userId>

# Or use npm script
npm run user-stats
npm run user-stats -- --user <userId>
```

### What It Shows

For each user, displays:
- **Email** - user's email address from `/account/email`
- **Display name** - user's display name from `/account/displayName`
- **Number of unique players** - count of unique player names in `/playersGames/`
- **Number of games** - count of games in `/games/`
- **Last game date** - the most recent game date from `dateString` field

### Examples

**View all users:**
```bash
node user-stats.js
```

Output:
```
═══════════════════════════════════════════════════════════════════════════════════════════════════════
 Email                                │ Display Name         │ Players │ Games │ Last Game Date
──────────────────────────────────────┼──────────────────────┼─────────┼───────┼──────────────────
 john.doe@example.com                 │ John Doe             │      15 │   247 │ 2026-01-15
 jane.smith@example.com               │ Jane Smith           │       8 │    89 │ 2026-01-10
═══════════════════════════════════════════════════════════════════════════════════════════════════════
```

**View specific user:**
```bash
node user-stats.js --user "abc123def456"
```

Output:
```
════════════════════════════════════════════════════════════════════
Email:              john.doe@example.com
Display name:       John Doe
Number of players:  15
Number of games:    247
Last game date:     2026-01-15
Last game ID:       247
════════════════════════════════════════════════════════════════════
```

## check-tie-result-bug.js

**Read-only** audit for the historical tie bug: when a game is a draw (`team1Score` = `team2Score`), `PlayerGame` rows for Team1/Team2 should not still show `result` = Lose. The app migration fixes local SQLite; this script measures how much bad data exists **in the Realtime Database** per user.

### Usage

```bash
cd scripts
npm install   # once

# All users (skips known non-user roots like `configurations`)
node check-tie-result-bug.js

# One Firebase Auth UID
node check-tie-result-bug.js --user <userId>

# Or npm
npm run check-tie-bug
npm run check-tie-bug -- --user <userId>
```

### What it reports

| Column | Meaning |
|--------|---------|
| **Ties** | Games where both sides have the same score (tie games). |
| **BugRows** | `playersGames` rows on those games where `team` is Team1/Team2 and `result` is Lose (`-1` or `"Lose"`). |
| **Plrs** | Distinct player names with at least one such row. |
| **Max bug date** | Latest `games.dateString` among games that have at least one buggy row (helps see how recent bad data is). |
| **Evident?** | `YES` if BugRows > 0 for that user. |

No writes are performed. Use the same `serviceAccountKey.json` setup as `user-stats.js`.

## copy-games.js

Copy games from one user to another. **INSERT ONLY** - never overwrites existing data.

### Usage

```bash
node copy-games.js --from <sourceUserId> --to <targetUserId> --start <startGameId> --end <endGameId> [--dry-run]
```

### Arguments

| Argument | Required | Description |
|----------|----------|-------------|
| `--from <userId>` | Yes | Source user UID (copy FROM this user) |
| `--to <userId>` | Yes | Target user UID (copy TO this user) |
| `--start <gameId>` | Yes | Start game ID (inclusive) |
| `--end <gameId>` | Yes | End game ID (inclusive) |
| `--dry-run` | No | Preview changes without writing to database |
| `--help` | No | Show help message |

### Examples

**Step 1: Always preview first with --dry-run:**
```bash
node copy-games.js --from "userB_uid" --to "userA_uid" --start 1 --end 300 --dry-run
```

**Step 2: Test with a fake/test user first:**
```bash
node copy-games.js --from "userB_uid" --to "test_copy_target" --start 1 --end 10
```

**Step 3: Actually copy to the real target:**
```bash
node copy-games.js --from "userB_uid" --to "userA_uid" --start 1 --end 300
```

### Behavior

- **INSERT ONLY**: Existing games in the target are always skipped, never overwritten
- **Creates target user**: If the target user doesn't exist, it will be created
- **Copies both games and player-games**: Copies from `/userId/games/` and `/userId/playersGames/`
- **Safe to run multiple times**: Since it only inserts, you can run it repeatedly

### What Gets Copied

| Source Path | Target Path | Notes |
|-------------|-------------|-------|
| `/{from}/games/{gameId}` | `/{to}/games/{gameId}` | Game metadata (date, scores, teams) |
| `/{from}/playersGames/{player}/{gameId}` | `/{to}/playersGames/{player}/{gameId}` | Player participation records |

### What Does NOT Get Copied

- Player records (`/userId/players/`) - players should already exist
- Account data (`/userId/account/`)
- App data (`/userId/app/`)

## Finding User UIDs

1. Go to [Firebase Console](https://console.firebase.google.com/project/teampickerv1/database)
2. In Realtime Database, the top-level keys are user UIDs
3. Or check Authentication > Users for the UID column

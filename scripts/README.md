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

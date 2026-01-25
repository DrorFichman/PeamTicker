package com.teampicker.drorfichman.teampicker.tools.analytics;

public enum EventType {
    main_players_tab, main_games_tab, main_insights_tab, // Done
    player_details_tab, player_games_tab, player_insights_tab, player_team_tab, // Done
    view_game, edit_game, delete_game, copy_game, // Done
    new_player, player_clicked, player_collaboration_clicked, // Done
    make_teams, send_teams, save_results, shuffle_teams, move_player, analysis_mode, analysis_mode_player_clicked, // Done
    settings_view, settings_changed_color, settings_changed_grades, settings_changed_tutorial_reset, settings_changed_show_hints, settings_changed_division_percentage, settings_changed_division_attempts, // Done
    backup_to_file, import_from_file, sync_to_cloud, pull_from_cloud, // Done
    paste_players, clear_attendance, players_archive, import_contacts, // Done
    sign_in, sign_out, // Done
    tutorial_dismissed, // Done
    db_migrate_empty_result, // Done
    in_app_review_requested, // Done
    in_app_review_shown, // Done
    in_app_review_completed, // Done

    // TODO
    search_players, search_insights,
    insights_games_range,
    tutorial_completed,
}

Embulk::JavaPlugin.register_input(
  "google_spreadsheets", "org.embulk.input.google_spreadsheets.GoogleSpreadsheetsInputPlugin",
  File.expand_path('../../../../classpath', __FILE__))

package org.embulk.input.google_spreadsheets;

import java.io.IOException;
import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Arrays;
import java.security.GeneralSecurityException;
import org.slf4j.Logger;
import com.google.common.base.Optional;
import org.embulk.config.TaskReport;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.spi.Exec;
import org.embulk.spi.InputPlugin;
import org.embulk.config.ConfigInject;
import org.embulk.spi.Schema;
import org.embulk.spi.PageOutput;
import org.embulk.spi.SchemaConfig;
import org.embulk.spi.TransactionalPageOutput;
import org.embulk.spi.Page;
import org.embulk.spi.Column;
import org.embulk.spi.ColumnVisitor;
import org.embulk.spi.PageReader;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.BufferAllocator;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.*;
import com.google.gdata.util.ServiceException;
import com.google.api.services.drive.DriveScopes;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.http.HttpTransport;

public class GoogleSpreadsheetsInputPlugin
        implements InputPlugin
{
    public interface PluginTask
            extends Task
    {
        @Config("service_account_email")
        public String getServiceAccountEmail();

        @Config("spreadsheet_id")
        public String getSpreadsheetId();

        @Config("p12_keyfile")
        public String getP12Keyfile();

        @Config("sheet_index")
        @ConfigDefault("0")
        public int getSheetIndex();

        @Config("application_name")
        @ConfigDefault("\"Embulk-GoogleSpreadsheets-InputPlugin\"")
        public String getApplicationName();

        @Config("columns")
        public SchemaConfig getColumns();

        @ConfigInject
        public BufferAllocator getBufferAllocator();
    }

    private final Logger log = Exec.getLogger(GoogleSpreadsheetsInputPlugin.class);

    @Override
    public ConfigDiff transaction(ConfigSource config,
            InputPlugin.Control control)
    {
        PluginTask task = config.loadConfig(PluginTask.class);

        Schema schema = task.getColumns().toSchema();
        int taskCount = 1;  // number of run() method calls

        return resume(task.dump(), schema, taskCount, control);
    }

    @Override
    public ConfigDiff resume(TaskSource taskSource,
            Schema schema, int taskCount,
            InputPlugin.Control control)
    {
        control.run(taskSource, schema, taskCount);
        return Exec.newConfigDiff();
    }

    @Override
    public void cleanup(TaskSource taskSource,
            Schema schema, int taskCount,
            List<TaskReport> successTaskReports)
    {
        // do nothing
    }

    @Override
    public TaskReport run(TaskSource taskSource,
            Schema schema, int taskIndex,
            PageOutput output)
    {
        PluginTask task = taskSource.loadTask(PluginTask.class);
        BufferAllocator allocator = task.getBufferAllocator();
        PageBuilder pageBuilder = new PageBuilder(allocator, schema, output);
        List<Column> columns = pageBuilder.getSchema().getColumns();

        try {
            GoogleCredential credentials = getServiceAccountCredential(task);
            SpreadsheetService service = new SpreadsheetService(task.getApplicationName());
            service.setProtocolVersion(SpreadsheetService.Versions.V3);
            service.setOAuth2Credentials(credentials);

            URL entryUrl = new URL("https://spreadsheets.google.com/feeds/spreadsheets/" + task.getSpreadsheetId());
            SpreadsheetEntry spreadsheet = service.getEntry(entryUrl, SpreadsheetEntry.class);

            WorksheetFeed worksheetFeed = service.getFeed(spreadsheet.getWorksheetFeedUrl(), WorksheetFeed.class);
            List<WorksheetEntry> worksheets = worksheetFeed.getEntries();
            WorksheetEntry worksheet = worksheets.get(task.getSheetIndex());

            URL listFeedUrl = worksheet.getListFeedUrl();
            ListFeed feed = service.getFeed(listFeedUrl, ListFeed.class);

            for (ListEntry row : feed.getEntries()) {
                int i = 0;
                for (String tag : row.getCustomElements().getTags()) {
                    pageBuilder.setString(columns.get(i), row.getCustomElements().getValue(tag));
                    i++;
                }
                pageBuilder.addRecord();
            }
        }
        catch (ServiceException e) {
            throw new RuntimeException(e);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }

        pageBuilder.finish();

        return Exec.newTaskReport();
    }

    @Override
    public ConfigDiff guess(ConfigSource config)
    {
        return Exec.newConfigDiff();
    }

    private GoogleCredential getServiceAccountCredential(PluginTask task)
            throws IOException, GeneralSecurityException
    {
        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        List<String> scopes = Arrays.asList(DriveScopes.DRIVE, "https://spreadsheets.google.com/feeds");

        return new GoogleCredential.Builder().setTransport(httpTransport)
                .setJsonFactory(jsonFactory)
                .setServiceAccountId(task.getServiceAccountEmail())
                .setServiceAccountPrivateKeyFromP12File(new File(task.getP12Keyfile()))
                .setServiceAccountScopes(scopes)
                .build();
    }
}

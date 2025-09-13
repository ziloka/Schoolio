import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.auth.oauth2.UserCredentials;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;

// @SpringBootApplication
@RestController
public class SyllabusController {

    private static final String APPLICATION_NAME = "Syllabus to Calendar";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList("https://www.googleapis.com/auth/calendar.events");

    // /**
    //  * Main application entry point.
    //  * @param args Command line arguments.
    //  */
    // public static void main(String[] args) {
    //     SpringApplication.run(SyllabusController.class, args);
    // }

    /**
     * Endpoint to upload a syllabus PDF and create calendar events.
     * @param file The multipart file containing the PDF syllabus.
     * @param startDate The start date of classes in 'MM-dd-yyyy' format.
     * @return A ResponseEntity with a success message or an error message.
     */
    @PostMapping("/upload-syllabus")
    public ResponseEntity<String> uploadSyllabus(@RequestParam("file") MultipartFile file,
                                                 @RequestParam(value = "startDate", required = false) String startDate) {
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please upload a file.");
        }

        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper textStripper = new PDFTextStripper();
            String text = textStripper.getText(document);

            // Handle the case where the start date is not provided
            LocalDate firstDayOfClass;
            if (startDate != null && !startDate.isEmpty()) {
                firstDayOfClass = LocalDate.parse(startDate, DateTimeFormatter.ofPattern("MM-dd-yyyy"));
            } else {
                // Use a default or ask the user for a specific date.
                // In a production app, you would prompt the user. For this example, we'll use today.
                firstDayOfClass = LocalDate.now();
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Start date not provided. Please provide the first day of class in MM-dd-yyyy format.");
            }

            // Create Google Calendar service and create events
            Calendar calendarService = getCalendarService();
            List<String> eventIds = createEventsFromSyllabus(text, firstDayOfClass, calendarService);

            return ResponseEntity.status(HttpStatus.OK).body("Successfully created " + eventIds.size() + " events.");

        } catch (IOException e) {
            System.err.println("Error reading PDF file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing file.");
        } catch (GeneralSecurityException e) {
            System.err.println("Google API authentication error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Authentication failed with Google API.");
        }
    }

    /**
     * Creates Google Calendar events by parsing the syllabus text.
     * This is a simplified parser and will need to be adapted for different syllabus formats.
     * @param syllabusText The extracted text from the PDF.
     * @param firstDayOfClass The date of the first day of class.
     * @param calendarService The Google Calendar service.
     * @return A list of created event IDs.
     * @throws IOException
     */
    private List<String> createEventsFromSyllabus(String syllabusText, LocalDate firstDayOfClass, Calendar calendarService) throws IOException {
        List<String> createdEventIds = new ArrayList<>();
        
        // This regex looks for "Week" followed by a number and then captures everything until the next "Week" or the end of the document.
        // It's a very simple pattern and may need to be adjusted.
        Pattern weekPattern = Pattern.compile("Week\\s*(\\d+)(.*?)(?=Week\\s*\\d+|\\Z)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher matcher = weekPattern.matcher(syllabusText);

        while (matcher.find()) {
            int weekNumber = Integer.parseInt(matcher.group(1));
            String weekContent = matcher.group(2);

            // Calculate the date for this week based on the start date
            // Assuming each week starts on the same day of the week as the first day of class
            LocalDate weekStartDate = firstDayOfClass.plusWeeks(weekNumber - 1);

            // A simple regex to find assignments or readings.
            // This is a placeholder and should be made more robust
            Pattern assignmentPattern = Pattern.compile("(Assignment|Reading|Due):\\s*(.*?)(?=\\n)", Pattern.CASE_INSENSITIVE);
            Matcher assignmentMatcher = assignmentPattern.matcher(weekContent);

            while (assignmentMatcher.find()) {
                String eventTitle = assignmentMatcher.group(2).trim();
                LocalDate eventDate = weekStartDate; // A simple assumption for now, could be improved

                // Create the event
                Event event = new Event()
                    .setSummary(eventTitle)
                    .setDescription("Syllabus event for Week " + weekNumber);

                EventDateTime start = new EventDateTime()
                    .setDate(new com.google.api.client.util.DateTime(eventDate.toString()));
                event.setStart(start);

                EventDateTime end = new EventDateTime()
                    .setDate(new com.google.api.client.util.DateTime(eventDate.toString()));
                event.setEnd(end);

                // Insert the event into the user's primary calendar
                String calendarId = "primary"; // Or the user's specific calendar ID
                Event createdEvent = calendarService.events().insert(calendarId, event).execute();
                System.out.println("Created event: " + createdEvent.getHtmlLink());
                createdEventIds.add(createdEvent.getId());
            }
        }
        return createdEventIds;
    }

    /**
     * Creates and returns a Google Calendar service.
     * IMPORTANT: This method assumes a service account or user credentials are set up.
     * You need to replace this with your actual authentication logic.
     * @return Calendar service.
     * @throws IOException
     * @throws GeneralSecurityException
     */
    private Calendar getCalendarService() throws IOException, GeneralSecurityException {
        // --- AUTHENTICATION ---
        // This is a placeholder for your authentication logic.
        // You would typically use a service account or an OAuth 2.0 flow.
        // For local testing, you can use a service account JSON file.
        // Download your service account key file from the Google Cloud Console and save it.
        // Example with Service Account:
        // GoogleCredentials credentials = ServiceAccountCredentials.fromStream(
        //         getClass().getResourceAsStream("/path/to/your-service-account-key.json"));
        // Example with User Credentials:
        // You'll need to set up OAuth 2.0 and handle the user consent flow.
        // For a simple local application, you can use the OAuth 2.0 flow with a localhost redirect.

        // Placeholder for credentials - YOU MUST REPLACE THIS
        GoogleCredentials credentials;
        try (InputStream credentialsStream = getClass().getResourceAsStream("/credentials.json")) {
            credentials = GoogleCredentials.fromStream(credentialsStream).createScoped(SCOPES);
        } catch (Exception e) {
            throw new IOException("Could not load Google credentials from 'credentials.json'. " +
                                  "Please ensure the file is in your classpath and contains valid credentials.", e);
        }

        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        return new Calendar.Builder(httpTransport, JSON_FACTORY, new HttpCredentialsAdapter(credentials))
            .setApplicationName(APPLICATION_NAME)
            .build();
    }
}

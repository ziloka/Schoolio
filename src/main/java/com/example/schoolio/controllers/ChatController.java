package com.example.schoolio.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.Builder;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;

// import org.springframework.ai.model.tool.ToolInvocation;
// import org.springframework.ai.model.tool.ToolCall;
// import org.springframework.ai.model.tool.ToolResult;
// import org.springframework.ai.model.tool.;;

import java.util.List;
import java.util.function.Function;
/**
 * REST Controller to expose a chat endpoint.
 */
@RestController
class ChatController {

    private final ChatClient chatClient;

    /**
     * We inject the ChatClient here. It is configured by Spring Boot to work with
     * the AI provider defined in application.properties (e.g., OpenAI, Google, etc.).
     */
    public ChatController(ChatClient.Builder chatClientBuilder, List<Function<?, ?>> toolFunctions) {
        // Here we build the ChatClient and register the tool functions.
        // This makes the "getCurrentWeather" function available to the LLM.
        this.chatClient = chatClientBuilder.build();
    }
    
    @GetMapping("/chat")
    public String chat(@RequestParam String message) {
        // The Prompt object encapsulates the user's message and the functions
        // available for tool calling.
        Prompt prompt = new Prompt(new UserMessage(message));
        
        // Send the prompt to the chat client. Spring AI and the LLM will
        // determine if a function call is needed.
        ChatResponse response = chatClient.prompt(prompt).call().chatResponse();
        
        // Return the generated content from the response.
        return response.getResult().getOutput().getText();
    }

    @PostMapping(value = "/chat/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadSyllabus(@RequestParam("syllabus") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("No file uploaded");
        }

        try {
            String content = new String(file.getBytes());

            String promptText = "You are a student planner assistant. Read the following syllabus and extract tasks with due dates and priorities:\n\n" + content;
            Prompt prompt = new Prompt(new UserMessage(promptText));

            ChatResponse response = chatClient.prompt(prompt).call().chatResponse();
            String result = response.getResult().getOutput().getText();

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error processing syllabus");
        }
    }

}
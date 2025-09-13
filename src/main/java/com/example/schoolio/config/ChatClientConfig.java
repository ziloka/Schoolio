package com.example.schoolio.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    // @Bean
    // public ChatClient.Builder chatClientBuilder(ChatModel chatModel) {
    //     // You would typically inject a ChatModel here,
    //     // which could be an OpenAiChatModel, AzureOpenAiChatModel, etc.
    //     return ChatClient.builder(chatModel);
    // }

    // // You would also need to define a ChatModel bean, for example:
    // @Bean
    // public ChatModel openAiChatModel(OpenAiApi openAiApi) {
    //     return new OpenAiChatModel(openAiApi);
    // }

}
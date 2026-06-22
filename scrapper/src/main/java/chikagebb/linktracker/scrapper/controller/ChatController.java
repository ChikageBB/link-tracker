package chikagebb.linktracker.scrapper.controller;

import chikagebb.linktracker.scrapper.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tg-chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/{chatId}")
    public ResponseEntity<Void> register(@PathVariable Long chatId) {
        chatService.registry(chatId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{chatId}")
    public ResponseEntity<Void> delete(@PathVariable Long chatId) {
        chatService.delete(chatId);
        return ResponseEntity.ok().build();
    }
}

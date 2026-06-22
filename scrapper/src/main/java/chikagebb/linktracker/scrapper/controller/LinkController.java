package chikagebb.linktracker.scrapper.controller;

import chikagebb.linktracker.scrapper.model.AddLinkRequest;
import chikagebb.linktracker.scrapper.model.LinkResponse;
import chikagebb.linktracker.scrapper.model.ListLinksResponse;
import chikagebb.linktracker.scrapper.model.RemoveLinkRequest;
import chikagebb.linktracker.scrapper.service.LinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/links")
@RequiredArgsConstructor
public class LinkController {

    private final LinkService linkService;

    @GetMapping
    public ResponseEntity<ListLinksResponse> getLinks(@RequestHeader("Tg-Chat-Id") Long chatId) {
        return ResponseEntity.ok(linkService.getAll(chatId));
    }

    @PostMapping
    public ResponseEntity<LinkResponse> addLink(
            @RequestHeader("Tg-Chat-Id") Long chatId, @Validated @RequestBody AddLinkRequest request) {
        return ResponseEntity.ok(linkService.add(chatId, request));
    }

    @DeleteMapping
    public ResponseEntity<LinkResponse> removeLink(
            @RequestHeader("Tg-Chat-Id") Long chatId, @Valid @RequestBody RemoveLinkRequest request) {
        return ResponseEntity.ok(linkService.delete(chatId, request));
    }
}

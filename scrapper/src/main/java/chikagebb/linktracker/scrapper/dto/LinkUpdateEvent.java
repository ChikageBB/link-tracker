package chikagebb.linktracker.scrapper.dto;

public record LinkUpdateEvent(Long id, String url, String description, Long tgChatId) {}

package chikagebb.linktracker.bot.service;

import static chikagebb.linktracker.bot.grpc.UpdateServiceGrpc.*;

import chikagebb.linktracker.bot.grpc.SendUpdateRequest;
import chikagebb.linktracker.bot.grpc.SendUpdateResponse;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UpdateGrpcService extends UpdateServiceImplBase {

    private final TelegramBot telegramBot;

    @Override
    public void sendUpdate(SendUpdateRequest request, StreamObserver<SendUpdateResponse> responseObserver) {
        log.atInfo()
                .setMessage("grpc.update.received")
                .addKeyValue("url", request.getUrl())
                .log();

        for (long chatId : request.getTgChatIdsList()) {
            telegramBot.execute(new SendMessage(chatId, request.getDescription()));
        }
        responseObserver.onNext(SendUpdateResponse.newBuilder().setStatus("OK").build());
        responseObserver.onCompleted();
    }
}

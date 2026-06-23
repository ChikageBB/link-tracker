package chikagebb.linktracker.scrapper.client;

import chikagebb.linktracker.scrapper.grpc.SendUpdateRequest;
import chikagebb.linktracker.scrapper.grpc.UpdateServiceGrpc;
import io.grpc.ManagedChannel;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.client.GrpcChannelFactory;

@Slf4j
public class BotGrpcClient implements NotificationClient {

    private final UpdateServiceGrpc.UpdateServiceBlockingStub stub;

    public BotGrpcClient(GrpcChannelFactory channelFactory) {
        ManagedChannel channel = channelFactory.createChannel("bot-grpc-service");
        this.stub = UpdateServiceGrpc.newBlockingStub(channel);
    }

    @Override
    public void sendUpdate(Long id, String url, String description, List<Long> chatIds) {
        try {
            var request = SendUpdateRequest.newBuilder()
                    .setId(id)
                    .setUrl(url)
                    .setDescription(description)
                    .addAllTgChatIds(chatIds)
                    .build();

            stub.sendUpdate(request);

            log.atInfo().setMessage("grpc.update.sent").addKeyValue("url", url).log();
        } catch (Exception e) {
            log.atError()
                    .setMessage("grpc.update.send.failed")
                    .addKeyValue("url", url)
                    .setCause(e)
                    .log();
        }
    }
}

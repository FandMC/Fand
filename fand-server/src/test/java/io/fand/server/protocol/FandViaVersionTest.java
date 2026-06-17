package io.fand.server.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import com.viaversion.viaversion.platform.ViaDecodeHandler;
import com.viaversion.viaversion.platform.ViaEncodeHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import java.nio.file.Path;
import net.minecraft.network.HandlerNames;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class FandViaVersionTest {

    @TempDir
    Path tempDir;

    @Test
    void leavesPipelineUntouchedWhenDisabled() {
        var via = new FandViaVersion(tempDir);
        var channel = channelWithFrameHandlers();

        via.inject(channel.pipeline());

        assertThat(channel.pipeline().get(ViaDecodeHandler.NAME)).isNull();
        assertThat(channel.pipeline().get(ViaEncodeHandler.NAME)).isNull();
        channel.finishAndReleaseAll();
    }

    @Test
    void addsViaHandlersWhenEnabled() {
        var via = new FandViaVersion(tempDir);
        var channel = channelWithFrameHandlers();

        via.configure(true);
        via.inject(channel.pipeline());
        via.inject(channel.pipeline());

        assertThat(channel.pipeline().get(ViaDecodeHandler.NAME)).isNotNull();
        assertThat(channel.pipeline().get(ViaEncodeHandler.NAME)).isNotNull();
        assertThat(channel.pipeline().names())
                .containsSubsequence(HandlerNames.SPLITTER, ViaDecodeHandler.NAME)
                .containsSubsequence(HandlerNames.PREPENDER, ViaEncodeHandler.NAME);
        assertThat(channel.pipeline().names().stream().filter(ViaDecodeHandler.NAME::equals)).hasSize(1);
        assertThat(channel.pipeline().names().stream().filter(ViaEncodeHandler.NAME::equals)).hasSize(1);
        channel.finishAndReleaseAll();
    }

    private static EmbeddedChannel channelWithFrameHandlers() {
        var channel = new EmbeddedChannel();
        channel.pipeline().addLast(HandlerNames.SPLITTER, new ChannelInboundHandlerAdapter());
        channel.pipeline().addLast(HandlerNames.PREPENDER, new ChannelInboundHandlerAdapter());
        return channel;
    }
}

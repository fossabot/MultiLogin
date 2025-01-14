package moe.caa.multilogin.dataupgrade.newc.yggdrasil.hasjoined;

import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

/**
 * HasJoined 唯一 custom 实现类
 */
public class Custom implements IHasJoined {
    private final String url;
    private final HttpMethod method;
    private final String ipContent;
    private final String postContent;

    public Custom(String url, HttpMethod method, String ipContent, String postContent) {
        this.url = url;
        this.method = method;
        this.ipContent = ipContent;
        this.postContent = postContent;
    }

    @Override
    public CommentedConfigurationNode toYaml() throws SerializationException {
        CommentedConfigurationNode ret = CommentedConfigurationNode.root();
        ret.node("custom").node("url").set(url.trim().replace("{passIpContent}", "{ip}"));
        ret.node("custom").node("method").set(method.name());
        ret.node("custom").node("ipContent").set(ipContent);
        ret.node("custom").node("postContent").set(postContent);
        return ret;
    }

    public enum HttpMethod {
        GET, POST
    }
}

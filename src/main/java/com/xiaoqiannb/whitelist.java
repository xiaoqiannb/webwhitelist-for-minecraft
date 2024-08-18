package com.xiaoqiannb;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.Date;

public class whitelist extends JavaPlugin {
    private HttpServer server;

    @Override
    public void onEnable() {
        getLogger().info("白名单插件已部署完成，爱来自小千");
        startWebServer();
    }

    @Override
    public void onDisable() {
        getLogger().info("白名单插件已退出，如果并非本意，请立刻联系技术人员");
        stopWebServer();
    }

    private void startWebServer() {
        try {
            server = HttpServer.create(new InetSocketAddress(12138), 0);
            server.createContext("/apply", new ApplicationHandler(this));
            server.setExecutor(null); // creates a default executor
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopWebServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    public void addToWhitelist(String playerName) {
        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "whitelist add " + playerName);
            }
        }.runTask(this);
    }

    static class ApplicationHandler implements HttpHandler {
        private final whitelist plugin;

        public ApplicationHandler(whitelist plugin) {
            this.plugin = plugin;
        }

        private String generateCaptcha() {
            SimpleDateFormat dateFormat = new SimpleDateFormat("ddHH");
            String dateString = dateFormat.format(new Date());
            int captchaValue = Integer.parseInt(dateString);
            captchaValue = captchaValue * 12;//验证码算法设置
            return String.valueOf(captchaValue);
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                String query = new String(exchange.getRequestBody().readAllBytes());
                String[] params = query.split("&");
                String playerName = null;
                String userCaptcha = null;
                for (String param : params) {
                    String[] keyValue = param.split("=");
                    if ("playerName".equals(keyValue[0])) {
                        playerName = keyValue[1];
                    } else if ("captcha".equals(keyValue[0])) {
                        userCaptcha = keyValue[1];
                    }
                }
                String captcha = generateCaptcha(); // 每次请求时重新生成验证码
                if (playerName != null && !playerName.isEmpty() && userCaptcha != null && userCaptcha.equals(captcha)) {
                    plugin.addToWhitelist(playerName);
                    String response = "success";
                    exchange.sendResponseHeaders(200, response.length());
                    exchange.getResponseBody().write(response.getBytes());
                } else {
                    String response = "error key";
                    exchange.sendResponseHeaders(400, response.length());
                    exchange.getResponseBody().write(response.getBytes());
                }
            } else {
                String response = "error";
                exchange.sendResponseHeaders(405, response.length());
                exchange.getResponseBody().write(response.getBytes());
            }
            exchange.getResponseBody().close();
        }
    }
}


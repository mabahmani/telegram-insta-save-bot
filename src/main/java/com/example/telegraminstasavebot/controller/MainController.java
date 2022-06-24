package com.example.telegraminstasavebot.controller;


import com.example.telegraminstasavebot.TelegramInstaSaveBotApplication;
import com.example.telegraminstasavebot.model.Media;
import com.example.telegraminstasavebot.model.MediaType;
import com.example.telegraminstasavebot.model.Update;
import com.example.telegraminstasavebot.model.User;
import com.example.telegraminstasavebot.okhttp.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;

@RestController
public class MainController {


    String[] numberEmojis = new String[10];
    String userEmoji = "\uD83D\uDC64";
    String captionEmoji = "\uD83D\uDCAC";
    String redDotEmoji = "\uD83D\uDD34";
    String cameraEmoji = "\uD83D\uDCF7";
    String movieCameraEmoji = "\uD83C\uDFA5";
    String infoEmoji = "\uD83D\uDD39";

    public MainController() {
        numberEmojis[0] = "1️⃣";
        numberEmojis[1] = "2️⃣";
        numberEmojis[2] = "3️⃣";
        numberEmojis[3] = "4️⃣";
        numberEmojis[4] = "5️⃣";
        numberEmojis[5] = "6️⃣";
        numberEmojis[6] = "7️⃣";
        numberEmojis[7] = "8️⃣";
        numberEmojis[8] = "9️⃣";
        numberEmojis[9] = "\uD83D\uDD1F";
    }

    @PostMapping("/")
    public List<Media> getUpdates(
            @RequestBody Update update
    ) {

        if (update.getMessage().getText().equals("/start")){
            sendMessage(
                    generateInfoMessageText("برای دریافت لینک دانلود، لینک کپی شده از اینستاگرام را پیست کنید.")
                    , update.getMessage().getChat().getId(), update.getMessage().getMessage_id());
        }

        else {
            Long messageId = sendMessage(
                    generateInfoMessageText("در حال بررسی لینک ...")
                    , update.getMessage().getChat().getId(), update.getMessage().getMessage_id());

            String text = update.getMessage().getText();

            if (checkIfMessageTextIsInstagramValidLink(text.toLowerCase())) {
                List<Media> medias = extractMediaLinks(text);
                if (medias != null && !medias.isEmpty()) {
                    editMessage(generateDownloadAllMessageText(medias), update.getMessage().getChat().getId(), messageId);
                    uploadMediasToTelegramAndSendMessage(medias, update.getMessage().getChat().getId(), update.getMessage().getMessage_id());
                } else {
                    editMessage(
                            generateErrorMessageText("مشکلی در پردازش لینک به وجود آمده است، ممکن است این لینک مربوط به یک پیج خصوصی باشد."),
                            update.getMessage().getChat().getId(), messageId
                    );
                }

                return extractMediaLinks(text);
            } else {
                editMessage(
                        generateErrorMessageText("لینک اشتباه است")
                        , update.getMessage().getChat().getId(), messageId
                );
                return new ArrayList<>();
            }
        }

        return new ArrayList<>();

    }

    private Boolean checkIfMessageTextIsInstagramValidLink(String text) {
        if (text.contains("instagram.com")) {
            try {
                new URL(text);
                return true;
            } catch (MalformedURLException exception) {
                return false;
            }
        } else {
            return false;
        }
    }

    private List<Media> extractMediaLinks(String text) {
        if (text.contains("stories")) {
            try {

                StringBuilder linkBuilder = new StringBuilder(text);

                if (linkBuilder.charAt(text.length() - 1) == '/') {
                    linkBuilder.setCharAt(text.length() - 1, '?');
                }

                String link = linkBuilder.toString();

                return extractStoryMedia(text.substring(link.lastIndexOf('/') + 1, link.indexOf('?')));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            try {
                if (text.contains("?")) {
                    return extractFeedMedia(text.substring(0, text.indexOf('?')));
                } else {
                    return extractFeedMedia(text);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return new ArrayList<>();
    }

    private List<Media> extractFeedMedia(String link) {

        List<Media> medias = new ArrayList<>();

        try {

            Response response = OkHttpClient.getInstance().newCall(
                    new Request.Builder()
                            .url(String.format("%s?__a=1&__d=dis", link)).build()
            ).execute();

            if (response.isSuccessful()) {
                if (response.body() != null) {

                    String jsonResponse = response.body().string();

                    JSONArray items = new JSONObject(jsonResponse).getJSONArray("items");

                    if (!items.isEmpty()) {

                        JSONObject item = items.getJSONObject(0);
                        String productType = item.getString("product_type");
                        String caption = item.getJSONObject("caption").getString("text");
                        String username = item.getJSONObject("user").getString("username");
                        String fullName = item.getJSONObject("user").getString("full_name");
                        int mediaType = item.getInt("media_type");
                        String pk = String.valueOf(item.get("pk"));

                        if (productType.equals("carousel_container") || mediaType == 8) {
                            JSONArray carouselMedias = item.getJSONArray("carousel_media");
                            for (int i = 0; i < carouselMedias.length(); i++) {
                                JSONObject carouselMedia = carouselMedias.getJSONObject(i);
                                int carouselMediaType = carouselMedia.getInt("media_type");
                                String carouselPk = String.valueOf(carouselMedia.get("pk"));
                                if (carouselMediaType == 1) {
                                    JSONArray images = carouselMedia.getJSONObject("image_versions2").getJSONArray("candidates");
                                    String bestQualityImageUrl = "";
                                    int bestWidth = 0;
                                    for (int j = 0; j < images.length(); j++) {
                                        JSONObject image = images.getJSONObject(j);
                                        if (image.getInt("width") > bestWidth) {
                                            bestWidth = image.getInt("width");
                                            bestQualityImageUrl = image.getString("url");
                                        }
                                    }

                                    Media media = new Media();
                                    media.setId(carouselPk);
                                    media.setMediaType(MediaType.IMAGE);
                                    media.setUrl(bestQualityImageUrl);
                                    media.setCaption(caption);
                                    User user = new User();
                                    user.setUsername(username);
                                    user.setFullName(fullName);
                                    media.setUser(user);

                                    medias.add(media);
                                } else if (carouselMediaType == 2) {
                                    JSONArray videos = carouselMedia.getJSONArray("video_versions");
                                    String bestQualityVideoUrl = "";
                                    int bestWidth = 0;

                                    for (int j = 0; j < videos.length(); j++) {
                                        JSONObject video = videos.getJSONObject(j);
                                        if (video.getInt("width") > bestWidth) {
                                            bestWidth = video.getInt("width");
                                            bestQualityVideoUrl = video.getString("url");
                                        }
                                    }

                                    Media media = new Media();
                                    media.setMediaType(MediaType.VIDEO);
                                    media.setId(carouselPk);
                                    media.setUrl(bestQualityVideoUrl);
                                    media.setCaption(caption);
                                    User user = new User();
                                    user.setUsername(username);
                                    user.setFullName(fullName);
                                    media.setUser(user);

                                    medias.add(media);
                                }

                            }
                        } else {

                            if (mediaType == 1) {
                                JSONArray images = item.getJSONObject("image_versions2").getJSONArray("candidates");
                                String bestQualityImageUrl = "";
                                int bestWidth = 0;
                                for (int j = 0; j < images.length(); j++) {
                                    JSONObject image = images.getJSONObject(j);
                                    if (image.getInt("width") > bestWidth) {
                                        bestWidth = image.getInt("width");
                                        bestQualityImageUrl = image.getString("url");
                                    }
                                }

                                Media media = new Media();
                                media.setId(pk);
                                media.setMediaType(MediaType.IMAGE);
                                media.setUrl(bestQualityImageUrl);
                                media.setCaption(caption);
                                User user = new User();
                                user.setUsername(username);
                                user.setFullName(fullName);
                                media.setUser(user);

                                medias.add(media);
                            } else if (mediaType == 2) {
                                JSONArray videos = item.getJSONArray("video_versions");
                                String bestQualityVideoUrl = "";
                                int bestWidth = 0;

                                for (int j = 0; j < videos.length(); j++) {
                                    JSONObject video = videos.getJSONObject(j);
                                    if (video.getInt("width") > bestWidth) {
                                        bestWidth = video.getInt("width");
                                        bestQualityVideoUrl = video.getString("url");
                                    }
                                }

                                Media media = new Media();
                                media.setId(pk);
                                media.setMediaType(MediaType.VIDEO);
                                media.setUrl(bestQualityVideoUrl);
                                media.setCaption(caption);
                                User user = new User();
                                user.setUsername(username);
                                user.setFullName(fullName);
                                media.setUser(user);

                                medias.add(media);
                            }
                        }
                    }
                }
            }

        } catch (IOException exception) {
            exception.printStackTrace();
        }


        return medias;
    }

    private List<Media> extractStoryMedia(String id) {

        List<Media> medias = new ArrayList<>();

        try {

            Response response = OkHttpClient.getInstance().newCall(
                    new Request.Builder()
                            .url(String.format("https://i.instagram.com/api/v1/media/%s/info/", id)).build()
            ).execute();

            if (response.isSuccessful()) {
                if (response.body() != null) {

                    String jsonResponse = response.body().string();

                    JSONArray items = new JSONObject(jsonResponse).getJSONArray("items");

                    if (!items.isEmpty()) {

                        JSONObject item = items.getJSONObject(0);
                        String productType = item.getString("product_type");
                        String caption = item.getJSONObject("caption").getString("text");
                        String username = item.getJSONObject("user").getString("username");
                        String fullName = item.getJSONObject("user").getString("full_name");
                        int mediaType = item.getInt("media_type");
                        String pk = String.valueOf(item.get("pk"));

                        if (mediaType == 1) {
                            JSONArray images = item.getJSONObject("image_versions2").getJSONArray("candidates");
                            String bestQualityImageUrl = "";
                            int bestWidth = 0;
                            for (int j = 0; j < images.length(); j++) {
                                JSONObject image = images.getJSONObject(j);
                                if (image.getInt("width") > bestWidth) {
                                    bestWidth = image.getInt("width");
                                    bestQualityImageUrl = image.getString("url");
                                }
                            }

                            Media media = new Media();
                            media.setId(pk);
                            media.setMediaType(MediaType.IMAGE);
                            media.setUrl(bestQualityImageUrl);
                            media.setCaption(caption);
                            User user = new User();
                            user.setUsername(username);
                            user.setFullName(fullName);
                            media.setUser(user);

                            medias.add(media);
                        } else if (mediaType == 2) {
                            JSONArray videos = item.getJSONArray("video_versions");
                            String bestQualityVideoUrl = "";
                            int bestWidth = 0;

                            for (int j = 0; j < videos.length(); j++) {
                                JSONObject video = videos.getJSONObject(j);
                                if (video.getInt("width") > bestWidth) {
                                    bestWidth = video.getInt("width");
                                    bestQualityVideoUrl = video.getString("url");
                                }
                            }

                            Media media = new Media();
                            media.setId(pk);
                            media.setMediaType(MediaType.VIDEO);
                            media.setUrl(bestQualityVideoUrl);
                            media.setCaption(caption);
                            User user = new User();
                            user.setUsername(username);
                            user.setFullName(fullName);
                            media.setUser(user);

                            medias.add(media);
                        }
                    }
                }
            }

        } catch (IOException exception) {
            exception.printStackTrace();
        }


        return medias;
    }

    private Long sendMessage(String text, Long chatId, Long replyId) {
        try {
            JSONObject bodyObject = new JSONObject();
            bodyObject.put("chat_id", chatId);
            bodyObject.put("text", text);
            bodyObject.put("reply_to_message_id", replyId);
            bodyObject.put("parse_mode", "markdown");
            okhttp3.RequestBody body = okhttp3.RequestBody.create(bodyObject.toString(), okhttp3.MediaType.parse("application/json; charset=utf-8"));
            Response response = OkHttpClient.getInstance().newCall(
                    new Request.Builder()
                            .post(body)
                            .url(String.format("https://api.telegram.org/bot%s/sendMessage", TelegramInstaSaveBotApplication.botToken)).build()
            ).execute();

            if (response.isSuccessful()) {
                String jsonResponse = response.body().string();
                return new JSONObject(jsonResponse).getJSONObject("result").getLong("message_id");
            }

        } catch (IOException exception) {
            exception.printStackTrace();
        }

        return -1L;

    }

    private Long editMessage(String text, Long chatId, Long messageId) {
        try {
            JSONObject bodyObject = new JSONObject();
            bodyObject.put("chat_id", chatId);
            bodyObject.put("text", text);
            bodyObject.put("message_id", messageId);
            bodyObject.put("parse_mode", "markdown");

            okhttp3.RequestBody body = okhttp3.RequestBody.create(bodyObject.toString(), okhttp3.MediaType.parse("application/json; charset=utf-8"));
            Response response = OkHttpClient.getInstance().newCall(
                    new Request.Builder()
                            .post(body)
                            .url(String.format("https://api.telegram.org/bot%s/editMessageText", TelegramInstaSaveBotApplication.botToken)).build()
            ).execute();

            if (response.isSuccessful()) {
                String jsonResponse = response.body().string();
                return new JSONObject(jsonResponse).getJSONObject("result").getLong("message_id");
            }

        } catch (IOException exception) {
            exception.printStackTrace();
        }

        return -1L;

    }

    private String generateDownloadAllMessageText(List<Media> medias) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(
                String.format("%s *%s* | *%s*\n\n%s %s\n\n", userEmoji, medias.get(0).getUser().getUsername(), medias.get(0).getUser().getFullName(), captionEmoji, medias.get(0).getCaption())
        );
        for (int i = 0; i < medias.size(); i++) {
            if (i < 10) {
                String mediaTypeEmoji = "";
                if (medias.get(i).getMediaType() == MediaType.IMAGE)
                    mediaTypeEmoji = cameraEmoji;
                else
                    mediaTypeEmoji = movieCameraEmoji;
                stringBuilder.append(
                        String.format("\n\n%s %s [%s](%s)", numberEmojis[i], mediaTypeEmoji, "لینک دانلود", medias.get(i).getUrl())
                );
            }
        }
        return stringBuilder.toString();
    }

    private String generateDownloadMediaMessageText(Media media) {
        StringBuilder stringBuilder = new StringBuilder();
        String mediaTypeEmoji = "";
        if (media.getMediaType() == MediaType.IMAGE)
            mediaTypeEmoji = cameraEmoji;
        else
            mediaTypeEmoji = movieCameraEmoji;

        stringBuilder.append(
                String.format("%s *%s* | *%s*\n\n%s [%s](%s)", userEmoji, media.getUser().getUsername(), media.getUser().getFullName(),mediaTypeEmoji,"لینک دانلود", media.getUrl())
        );

        return stringBuilder.toString();
    }

    private String generateErrorMessageText(String message) {
        return String.format("%s *%s*", redDotEmoji, message);
    }

    private String generateInfoMessageText(String message) {
        return String.format("%s *%s*", infoEmoji, message);
    }

    private void uploadMediasToTelegramAndSendMessage(List<Media> medias, Long chatId, Long replyId) {
        for (Media media : medias) {
            try {
                if (media.getMediaType() == MediaType.VIDEO) {
                    sendVideo(media.getUrl(), generateDownloadMediaMessageText(media), chatId, replyId);
                } else {
                    sendImage(media.getUrl(), generateDownloadMediaMessageText(media), chatId, replyId);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }

    private void sendVideo(String mediaUrl, String text, Long chatId, Long replyId) {
        try {

            JSONObject bodyObject = new JSONObject();
            bodyObject.put("chat_id", chatId);
            bodyObject.put("video", mediaUrl);
            bodyObject.put("caption", text);
            bodyObject.put("reply_to_message_id", replyId);
            bodyObject.put("parse_mode", "markdown");

            okhttp3.RequestBody body = okhttp3.RequestBody.create(bodyObject.toString(), okhttp3.MediaType.parse("application/json; charset=utf-8"));

            OkHttpClient.getInstance().newCall(
                    new Request.Builder()
                            .post(body)
                            .url(String.format("https://api.telegram.org/bot%s/sendVideo", TelegramInstaSaveBotApplication.botToken)).build()
            ).execute();


        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private void sendImage(String mediaUrl, String text, Long chatId, Long replyId) {
        try {

            JSONObject bodyObject = new JSONObject();
            bodyObject.put("chat_id", chatId);
            bodyObject.put("photo", mediaUrl);
            bodyObject.put("caption", text);
            bodyObject.put("reply_to_message_id", replyId);
            bodyObject.put("parse_mode", "markdown");

            okhttp3.RequestBody body = okhttp3.RequestBody.create(bodyObject.toString(), okhttp3.MediaType.parse("application/json; charset=utf-8"));

            OkHttpClient.getInstance().newCall(
                    new Request.Builder()
                            .post(body)
                            .url(String.format("https://api.telegram.org/bot%s/sendPhoto", TelegramInstaSaveBotApplication.botToken)).build()
            ).execute();


        } catch (IOException exception) {
            exception.printStackTrace();
        }

    }

}

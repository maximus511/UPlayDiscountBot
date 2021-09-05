package com.rahul.textcapture;

import com.google.cloud.vision.v1.*;
import com.google.cloud.vision.v1.Image;
import com.google.protobuf.ByteString;
import org.sikuli.basics.Settings;
import org.sikuli.script.*;
import sun.audio.AudioPlayer;
import sun.audio.AudioStream;


import javax.imageio.ImageIO;
import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Bot program to capture the text from a video playing on the screen.
 *
 * This can be easily modified to capture directly from a livestream but due to network latency, we could miss quite a few frames.
 * To avoid that, this program requires the video to be played while it captures the frame from the screen itself.
 */
public class CaptureTextFromVideo {

    private static final String SCREENPATH = "src\\main\\resources\\images\\";
    private static ArrayList<String> codeList = new ArrayList<>();

    /**
     * Alternate method using Sikuli to capture frame and parse text
     */
    public static void main(String[] args) throws FindFailed, InterruptedException, IOException {
        Settings.OcrTextRead = true;
        Settings.OcrTextSearch = true;
        ImagePath.add(System.getProperty("user.dir"));
        ImagePath.setBundlePath(System.getProperty("user.dir"));
        System.out.println(ImagePath.getBundlePath());
        Screen s = new Screen();
        //Code for starting the UPLAY application
        /*if(s.exists(SCREENPATH+"loggedIn_options.png") == null) {
            s.find(SCREENPATH+"uplaylogo.png"); //identify ubisoft logo button
            s.doubleClick(SCREENPATH+"uplaylogo.png");//double click ubisoft logo button
            System.out.println("UPlay opened");
        }
        long startTime = System.currentTimeMillis();
        while(s.exists(SCREENPATH+"uplay_menu.png") == null) {
            if (System.currentTimeMillis() - startTime > 20000) {
                break;
            }
        }*/
//        s.find(SCREENPATH+"uplay\\menu.png");
//        s.click(SCREENPATH+"uplay\\menu.png");
//        Thread.sleep(2000);
        HashSet<String> keys = new HashSet<>();
        InputStream successMusic, failMusic;

        successMusic = new FileInputStream(new File("src\\main\\resources\\sounds\\applause_y.wav"));
        failMusic = new FileInputStream(new File("src\\main\\resources\\sounds\\quick_fart_x.wav"));
        AudioStream successStream = new AudioStream(successMusic);
        AudioStream failStream = new AudioStream(failMusic);
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < 7200000L) {
            ImageIO.write(s.capture().getImage(), "png", new File("screen.png"));
            String uplayCode = detectText("screen.png");
            if (uplayCode == null || uplayCode.isEmpty() || !keys.add(uplayCode)) {
                continue;
            }
            System.out.println("UPLAY CODE TO BE ENTERED - " + uplayCode);
            if (s.exists(SCREENPATH + "uplay\\activatetxt.png") == null) {
                s.find(SCREENPATH + "uplay\\menu.png").click();
                Thread.sleep(1000);
                s.find(SCREENPATH + "uplay\\activatekey.png").click();
                Thread.sleep(1000);
            }

            s.find(SCREENPATH + "uplay\\activatetxt.png").click();
            s.paste(uplayCode);
            s.find(SCREENPATH + "uplay\\activatebtn.png").click();
            if (s.exists(SCREENPATH + "uplay\\activatebtn.png") != null) {
                AudioPlayer.player.start(failStream);
//                s.find(SCREENPATH + "uplay\\cancel.png").click();
                s.find(SCREENPATH + "uplay\\reset_Text.png").click();
                s.type("a", KeyModifier.CTRL);
                s.type(Key.DELETE);
                AudioPlayer.player.stop(failStream);
            } else {
                AudioPlayer.player.start(successStream);
            }
        }
    }

    /**
     * Detect the code text from the frame captured through Sikuli
     * @param filePath
     * @return
     * @throws IOException
     */
    private static String detectText(String filePath) throws IOException {
        List<AnnotateImageRequest> requests = new ArrayList<>();

        ByteString imgBytes = ByteString.readFrom(new FileInputStream(filePath));

        Image img = Image.newBuilder().setContent(imgBytes).build();
        Feature feat = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build();
        AnnotateImageRequest request =
                AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
        requests.add(request);

        // Initialize client to send requests - create and reuse for subsequent requests
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();

            for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                    System.out.format("Error: %s%n", res.getError().getMessage());
                    return null;
                }
                String regex = "\\w{4}-\\w{4}-\\w{4}-\\w{4}";
                Pattern pattern = Pattern.compile(regex);
                // For full list of available annotations, see http://g.co/cloud/vision/docs
                for (EntityAnnotation annotation : res.getTextAnnotationsList()) {
                    String[] textFromScreen = annotation.getDescription().split("\\n");
                    for (String text : textFromScreen) {
                        if (!text.contains("AAAA-BBBB-CCCC-DDDD") && !text.contains("AAA-BBBB-CCCC-DDDD-EEEE")) {
                            Matcher matcher = pattern.matcher(text);
                            if (matcher.find()) {
                                System.out.println(text);
                                return matcher.group();
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

}

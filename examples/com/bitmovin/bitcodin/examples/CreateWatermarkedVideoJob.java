package com.bitmovin.bitcodin.examples;

import com.bitmovin.bitcodin.api.BitcodinApi;
import com.bitmovin.bitcodin.api.exception.BitcodinApiException;
import com.bitmovin.bitcodin.api.input.HTTPInputConfig;
import com.bitmovin.bitcodin.api.input.Input;
import com.bitmovin.bitcodin.api.job.*;
import com.bitmovin.bitcodin.api.media.*;

public class CreateWatermarkedVideoJob {

    public static void main(String[] args) throws InterruptedException {
        
        /* Create BitcodinApi */
        String apiKey = "YOUR_API_KEY";
        BitcodinApi bitApi = new BitcodinApi(apiKey);
        
        /* Create URL Input */
        HTTPInputConfig httpInputConfig = new HTTPInputConfig();
        httpInputConfig.url = "http://eu-storage.bitcodin.com/inputs/Sintel.2010.720p.mkv";

        Input input;
        try {
            input = bitApi.createInput(httpInputConfig);
        } catch (BitcodinApiException e) {
            System.out.println("Could not create input: " + e.getMessage());
            return;
        }
        
        System.out.println("Created Input: " + input.filename);
        
        /* Create EncodingProfile */
        VideoStreamConfig videoConfig1 = new VideoStreamConfig();
        videoConfig1.bitrate = 4800000;
        videoConfig1.width = 1920;
        videoConfig1.height = 1080;
        videoConfig1.profile = Profile.MAIN;
        videoConfig1.preset = Preset.PREMIUM;

        VideoStreamConfig videoConfig2 = new VideoStreamConfig();
        videoConfig2.bitrate = 2400000;
        videoConfig2.width = 1280;
        videoConfig2.height = 720;
        videoConfig2.profile = Profile.MAIN;
        videoConfig2.preset = Preset.PREMIUM;

        VideoStreamConfig videoConfig3 = new VideoStreamConfig();
        videoConfig3.bitrate = 1200000;
        videoConfig3.width = 854;
        videoConfig3.height = 480;
        videoConfig3.profile = Profile.MAIN;
        videoConfig3.preset = Preset.PREMIUM;

        EncodingProfileConfig encodingProfileConfig = new EncodingProfileConfig();
        encodingProfileConfig.name = "JavaTestProfile";
        encodingProfileConfig.videoStreamConfigs.add(videoConfig1);
        encodingProfileConfig.videoStreamConfigs.add(videoConfig2);
        encodingProfileConfig.videoStreamConfigs.add(videoConfig3);

        AudioStreamConfig audioStreamConfig = new AudioStreamConfig();
        audioStreamConfig.defaultStreamId = 0;
        audioStreamConfig.bitrate = 192000;
        encodingProfileConfig.audioStreamConfigs.add(audioStreamConfig);

        /* CREATE WATERMARK */
        WatermarkConfig watermarkConfig = new WatermarkConfig();
        watermarkConfig.bottom = 200; // Watermark will be placed with a distance of 200 pixel to the bottom of the input video
        watermarkConfig.right = 100;  // Watermark will be placed with a distance of 100 pixel to the right of the input video
        watermarkConfig.image = "http://bitdash-a.akamaihd.net/webpages/bitcodin/images/bitcodin-bitmovin-logo-small.png";

        encodingProfileConfig.watermarkConfig = watermarkConfig;

        EncodingProfile encodingProfile;
        try {
            encodingProfile = bitApi.createEncodingProfile(encodingProfileConfig);
        } catch (BitcodinApiException e) {
            System.out.println("Could not create encoding profile: " + e.getMessage());
            return;
        }
        
        /* Create Job */
        JobConfig jobConfig = new JobConfig();
        jobConfig.encodingProfileId = encodingProfile.encodingProfileId;
        jobConfig.inputId = input.inputId;
        jobConfig.manifestTypes.addElement(ManifestType.MPEG_DASH_MPD);
        jobConfig.manifestTypes.addElement(ManifestType.HLS_M3U8);
        jobConfig.speed = Speed.STANDARD;

        Job job;
        try {
            job = bitApi.createJob(jobConfig);
        } catch (BitcodinApiException e) {
            System.out.println("Could not create job: " + e.getMessage());
            return;
        }
        
        JobDetails jobDetails;
        
        do {
            try {
                jobDetails = bitApi.getJobDetails(job.jobId);
                System.out.println("Status: " + jobDetails.status.toString() +
                                   " - Enqueued Duration: " + jobDetails.enqueueDuration + "s" +
                                   " - Realtime Factor: " + jobDetails.realtimeFactor +
                                   " - Encoded Duration: " + jobDetails.encodedDuration + "s" +
                                   " - Output: " + jobDetails.bytesWritten/1024/1024 + "MB");
            } catch (BitcodinApiException e) {
                System.out.println("Could not get any job details");
                return;
            }
            
            if (jobDetails.status == JobStatus.ERROR) {
                System.out.println("Error during transcoding");
                return;
            }
            
            Thread.sleep(2000);
            
        } while (jobDetails.status != JobStatus.FINISHED);
        
        System.out.println("Job with ID " + job.jobId + " finished successfully!");
    }
}

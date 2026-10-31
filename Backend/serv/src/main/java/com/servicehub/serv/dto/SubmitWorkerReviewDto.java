
package com.servicehub.serv.dto;

import java.util.List;

public class SubmitWorkerReviewDto {

    private int rating;
    private List<String> feedbackTags;

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public List<String> getFeedbackTags() {
        return feedbackTags;
    }

    public void setFeedbackTags(List<String> feedbackTags) {
        this.feedbackTags = feedbackTags;
    }
}

package com.wimoor.ozon.product.pojo.vo;

import java.util.List;

import lombok.Data;

@Data
public class OzonProductPreviewView {

    private EffectivePayloadSummary effectivePayloadSummary;
    private List<String> validationErrors;
    private List<VariantIssue> variantIssues;
    private boolean canPublish;

    @Data
    public static class EffectivePayloadSummary {
        private String draftId;
        private TopLevelFields topLevelFields;
        private List<EffectiveVariantSummary> variants;
    }

    @Data
    public static class TopLevelFields {
        private int offerIdCount;
        private boolean hasImages;
        private boolean hasDimensions;
    }

    @Data
    public static class EffectiveVariantSummary {
        private String variantId;
        private String materialSku;
        private String effectiveOfferId;
        private String effectiveBarcode;
        private String effectivePrice;
        private String effectiveWeight;
        private EffectiveDimensionSummary effectiveDimensions;
        private Integer effectiveImageCount;
    }

    @Data
    public static class EffectiveDimensionSummary {
        private String depth;
        private String width;
        private String height;
    }

    @Data
    public static class VariantIssue {
        private String variantId;
        private String materialSku;
        private List<String> messages;
    }
}

package com.businessos.modules.servicedesk.kb;

public class KbArticleMapper {

    public static KbArticleResponse toResponse(KbArticle article) {
        if (article == null) return null;

        KbArticleResponse response = new KbArticleResponse();
        response.setId(article.getId());
        response.setTitle(article.getTitle());
        response.setSummary(article.getSummary());
        response.setContent(article.getContent());
        response.setStatus(article.getStatus());
        response.setClientVisible(article.isClientVisible());
        response.setKeywords(article.getKeywords());
        response.setViewCount(article.getViewCount());
        response.setHelpfulCount(article.getHelpfulCount());
        response.setPublishedAt(article.getPublishedAt());

        if (article.getCategory() != null) {
            response.setCategoryId(article.getCategory().getId());
            response.setCategoryName(article.getCategory().getName());
        }
        if (article.getRelatedService() != null) {
            response.setRelatedServiceId(article.getRelatedService().getId());
            response.setRelatedServiceName(article.getRelatedService().getName());
        }
        if (article.getAuthor() != null) {
            response.setAuthorName(article.getAuthor().getFullName());
        }

        response.setCreatedAt(article.getCreatedAt());
        return response;
    }
}

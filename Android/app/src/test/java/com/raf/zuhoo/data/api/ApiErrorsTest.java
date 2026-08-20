package com.raf.zuhoo.data.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import okhttp3.MediaType;
import okhttp3.ResponseBody;

import org.junit.Test;

import retrofit2.Response;

/**
 * Error-body parsing. Two shapes arrive from the backend and they are not the same:
 * GlobalExceptionHandler's {success, message, data} envelope, and SubscriptionEnforcementFilter's
 * bare {error, message}. Confusing the second one for an ordinary permission failure is what the
 * subscription-expired flag exists to prevent.
 */
public class ApiErrorsTest {

    private static final String FALLBACK = "fallback";

    private Response<Object> errorResponse(int code, String json) {
        return Response.error(code,
                ResponseBody.create(json, MediaType.get("application/json")));
    }

    @Test
    public void usesTheBackendMessageWhenThereIsOne() {

        ApiErrors.ApiError error = ApiErrors.describe(
                errorResponse(400, "{\"success\":false,\"message\":\"Title is required\"}"), FALLBACK);

        assertEquals("Title is required", error.getMessage());
        assertFalse(error.isSubscriptionExpired());
    }

    @Test
    public void flattensFieldValidationErrors() {

        // A 400 from bean validation carries a field -> message map; the user needs the messages,
        // not the field names.
        ApiErrors.ApiError error = ApiErrors.describe(errorResponse(400,
                "{\"success\":false,\"message\":\"Validation failed\","
                        + "\"data\":{\"title\":\"Title is required\"}}"), FALLBACK);

        assertEquals("Title is required", error.getMessage());
    }

    @Test
    public void detectsTheSubscriptionExpiredFilter() {

        ApiErrors.ApiError error = ApiErrors.describe(errorResponse(403,
                "{\"error\":\"Subscription Expired\","
                        + "\"message\":\"Your subscription or trial has expired.\"}"), FALLBACK);

        assertTrue(error.isSubscriptionExpired());
        assertEquals("Your subscription or trial has expired.", error.getMessage());
    }

    @Test
    public void anOrdinaryForbiddenIsNotSubscriptionExpired() {

        ApiErrors.ApiError error = ApiErrors.describe(errorResponse(403,
                "{\"success\":false,\"message\":\"Access denied\"}"), FALLBACK);

        assertFalse(error.isSubscriptionExpired());
        assertEquals("Access denied", error.getMessage());
    }

    @Test
    public void subscriptionExpiredIsOnlyRecognisedOnA403() {

        // The marker only means what it means on the status code the filter actually returns.
        ApiErrors.ApiError error = ApiErrors.describe(errorResponse(500,
                "{\"error\":\"Subscription Expired\",\"message\":\"boom\"}"), FALLBACK);

        assertFalse(error.isSubscriptionExpired());
    }

    @Test
    public void fallsBackWhenTheBodyIsNotJson() {

        // Some endpoints return a bare string; parsing must not throw.
        ApiErrors.ApiError error = ApiErrors.describe(
                errorResponse(500, "Internal Server Error"), FALLBACK);

        assertEquals(FALLBACK, error.getMessage());
        assertFalse(error.isSubscriptionExpired());
    }

    @Test
    public void fallsBackWhenTheBodyIsEmpty() {

        ApiErrors.ApiError error = ApiErrors.describe(errorResponse(502, ""), FALLBACK);

        assertEquals(FALLBACK, error.getMessage());
    }
}

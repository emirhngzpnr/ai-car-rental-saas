package com.aicarrental.application.publicapi.ai;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class VehicleSearchHeuristicInterpreter {
    private static final Pattern KM_PATTERN =
            Pattern.compile("(?:en az|minimum|at least)?\\s*(\\d{2,4})\\s*(?:km|kilometre)");
    private static final Pattern SEAT_PATTERN =
            Pattern.compile("(\\d{1,2})\\s*(?:kisilik|koltuklu|koltuk|seats?|seater)");

    public VehicleSearchAiResult interpret(String query) {
        String normalized = normalize(query);
        String sort = detectSort(normalized);
        String priceIntent = detectPriceIntent(normalized, sort);
        String segmentIntent = detectSegmentIntent(normalized);
        String category = detectCategory(normalized);
        String transmission = detectTransmission(normalized);
        String fuelType = detectFuelType(normalized);
        String dateIntent = detectDateIntent(normalized);
        Integer minDailyKmLimit = extractInteger(KM_PATTERN, normalized);
        Integer minSeats = extractInteger(SEAT_PATTERN, normalized);

        boolean matched = sort != null
                || priceIntent != null
                || segmentIntent != null
                || category != null
                || transmission != null
                || fuelType != null
                || dateIntent != null
                || minDailyKmLimit != null
                || minSeats != null;

        return new VehicleSearchAiResult(
                null,
                null,
                minDailyKmLimit,
                null,
                null,
                category,
                transmission,
                fuelType,
                minSeats,
                null,
                sort,
                priceIntent,
                segmentIntent,
                dateIntent,
                null,
                null,
                List.of(),
                matched ? "Search preferences were interpreted from your request." : null,
                List.of()
        );
    }

    public VehicleSearchAiResult merge(VehicleSearchAiResult ai, VehicleSearchAiResult fallback) {
        if (ai == null) {
            return fallback;
        }
        boolean aiHasCriteria = hasCriteria(ai);
        return new VehicleSearchAiResult(
                ai.minDailyPrice(),
                ai.maxDailyPrice(),
                first(ai.minDailyKmLimit(), fallback.minDailyKmLimit()),
                first(ai.brand(), fallback.brand()),
                first(ai.model(), fallback.model()),
                first(ai.category(), fallback.category()),
                first(ai.transmission(), fallback.transmission()),
                first(ai.fuelType(), fallback.fuelType()),
                first(ai.minSeats(), fallback.minSeats()),
                first(ai.location(), fallback.location()),
                first(ai.sort(), fallback.sort()),
                first(ai.priceIntent(), fallback.priceIntent()),
                first(ai.segmentIntent(), fallback.segmentIntent()),
                first(ai.dateIntent(), fallback.dateIntent()),
                first(ai.pickupDateHint(), fallback.pickupDateHint()),
                first(ai.returnDateHint(), fallback.returnDateHint()),
                ai.missingFields() != null && !ai.missingFields().isEmpty() ? ai.missingFields() : fallback.missingFields(),
                aiHasCriteria ? first(ai.summary(), fallback.summary()) : fallback.summary(),
                aiHasCriteria && ai.warnings() != null ? ai.warnings() : fallback.warnings()
        );
    }

    public boolean hasCriteria(VehicleSearchAiResult result) {
        return result != null && (
                result.minDailyPrice() != null
                        || result.maxDailyPrice() != null
                        || result.minDailyKmLimit() != null
                        || notBlank(result.brand())
                        || notBlank(result.model())
                        || notBlank(result.category())
                        || notBlank(result.transmission())
                        || notBlank(result.fuelType())
                        || result.minSeats() != null
                        || notBlank(result.location())
                        || notBlank(result.sort())
                        || notBlank(result.priceIntent())
                        || notBlank(result.segmentIntent())
                        || notBlank(result.dateIntent())
                        || notBlank(result.pickupDateHint())
                        || notBlank(result.returnDateHint())
        );
    }

    private String detectSort(String query) {
        if (containsAny(query, "en ucuz", "cheapest", "lowest price", "fiyati en dusuk")) {
            return "priceAsc";
        }
        if (containsAny(query, "en pahali", "most expensive", "highest price")) {
            return "priceDesc";
        }
        if (containsAny(query, "en cok km", "en yuksek km", "highest km", "most mileage allowance")) {
            return "kmLimitDesc";
        }
        return null;
    }

    private String detectPriceIntent(String query, String sort) {
        if ("priceAsc".equals(sort) || "priceDesc".equals(sort)) {
            return null;
        }
        if (containsAny(query, "cok pahali olmayan", "pahali olmayan", "not too expensive", "not expensive")) {
            return PriceIntent.NOT_EXPENSIVE.name();
        }
        if (containsAny(query, "uygun fiyatli", "hesapli", "affordable", "reasonably priced")) {
            return PriceIntent.AFFORDABLE.name();
        }
        if (containsAny(query, "ucuz", "butce dostu", "budget", "cheap", "economical price")) {
            return PriceIntent.BUDGET.name();
        }
        if (containsAny(query, "premium fiyat", "pahali", "premium price", "high end price")) {
            return PriceIntent.PREMIUM.name();
        }
        return null;
    }

    private String detectSegmentIntent(String query) {
        if (containsAny(query, "orta segment", "mid range", "mid-range")) {
            return SegmentIntent.MID_RANGE.name();
        }
        if (containsAny(query, "aile araci", "aile arabasi", "aile icin", "family car", "for family")) {
            return SegmentIntent.FAMILY.name();
        }
        if (containsAny(query, "genis arac", "ferah", "spacious", "large interior")) {
            return SegmentIntent.SPACIOUS.name();
        }
        if (containsAny(query, "sehir ici", "sehir arabasi", "city car", "urban car")) {
            return SegmentIntent.CITY.name();
        }
        if (containsAny(query, "premium segment", "luks segment", "premium vehicle")) {
            return SegmentIntent.PREMIUM.name();
        }
        return null;
    }

    private String detectDateIntent(String query) {
        if (containsAny(query, "hafta sonu", "weekend")) {
            return DateIntent.WEEKEND.name();
        }
        if (containsAny(query, "den", "dan", "to", "until", "kadar")) {
            return DateIntent.DATE_RANGE.name();
        }
        if (containsAny(query, "bugun", "today", "yarin", "tomorrow",
                "pazartesi", "sali", "carsamba", "persembe", "cuma", "cumartesi", "pazar",
                "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")) {
            return DateIntent.PICKUP_ONLY.name();
        }
        return null;
    }

    private String detectCategory(String query) {
        if (containsAny(query, "suv")) return "SUV";
        if (containsAny(query, "sedan")) return "SEDAN";
        if (containsAny(query, "compact", "kompakt")) return "COMPACT";
        if (containsAny(query, "economy", "ekonomi sinifi")) return "ECONOMY";
        if (containsAny(query, "luxury", "luks")) return "LUXURY";
        if (containsAny(query, "van", "minivan")) return "VAN";
        return null;
    }

    private String detectTransmission(String query) {
        if (containsAny(query, "otomatik", "automatic")) return "AUTOMATIC";
        if (containsAny(query, "manuel", "manual")) return "MANUAL";
        return null;
    }

    private String detectFuelType(String query) {
        if (containsAny(query, "elektrikli", "electric")) return "ELECTRIC";
        if (containsAny(query, "hibrit", "hybrid")) return "HYBRID";
        if (containsAny(query, "dizel", "diesel")) return "DIESEL";
        if (containsAny(query, "benzinli", "gasoline", "petrol")) return "GASOLINE";
        if (containsAny(query, "lpg")) return "LPG";
        return null;
    }

    private Integer extractInteger(Pattern pattern, String query) {
        Matcher matcher = pattern.matcher(query);
        return matcher.find() ? Integer.valueOf(matcher.group(1)) : null;
    }

    private String normalize(String value) {
        String lowered = value.toLowerCase(Locale.forLanguageTag("tr"));
        String ascii = Normalizer.normalize(lowered, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return ascii
                .replace('\u0131', 'i')
                .replace('\u015f', 's')
                .replace('\u011f', 'g')
                .replace('\u00fc', 'u')
                .replace('\u00f6', 'o')
                .replace('\u00e7', 'c');
    }

    private boolean containsAny(String value, String... candidates) {
        for (String candidate : candidates) {
            if (value.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private <T> T first(T primary, T fallback) {
        if (primary instanceof String string && string.isBlank()) {
            return fallback;
        }
        return primary != null ? primary : fallback;
    }
}

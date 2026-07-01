package com.aicarrental.application.publicapi;

import com.aicarrental.api.publicapi.response.PublicVehicleSearchCriteriaResponse;
import com.aicarrental.api.publicapi.response.PublicVehicleSearchDateCriteriaResponse;
import com.aicarrental.api.publicapi.response.PublicVehicleSearchInterpretationResponse;
import com.aicarrental.api.publicapi.response.PublicVehicleSearchInterpretResponse;
import com.aicarrental.application.publicapi.ai.DateIntent;
import com.aicarrental.application.publicapi.ai.PriceIntent;
import com.aicarrental.application.publicapi.ai.SegmentIntent;
import com.aicarrental.application.publicapi.ai.VehicleSearchAiClient;
import com.aicarrental.application.publicapi.ai.VehicleSearchHeuristicInterpreter;
import com.aicarrental.application.publicapi.ai.VehicleSearchAiResult;
import com.aicarrental.common.exception.BusinessException;
import com.aicarrental.common.exception.ServiceUnavailableException;
import com.aicarrental.domain.vehicle.FuelType;
import com.aicarrental.domain.vehicle.TransmissionType;
import com.aicarrental.domain.vehicle.VehicleCategory;
import com.aicarrental.infrastructure.persistence.VehiclePriceDistributionProjection;
import com.aicarrental.infrastructure.persistence.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicVehicleSearchInterpretationService {
    private static final int MAX_QUERY_LENGTH = 500;
    private static final int MAX_TEXT_LENGTH = 80;
    private static final int MAX_SUMMARY_LENGTH = 240;
    private static final int MAX_WARNING_LENGTH = 160;
    private static final int MAX_WARNINGS = 6;
    private static final long MIN_PRICE_SAMPLE_SIZE = 3;
    private static final LocalTime DEFAULT_PICKUP_TIME = LocalTime.of(10, 0);
    private static final LocalTime DEFAULT_RETURN_TIME = LocalTime.of(10, 0);
    private static final LocalTime MORNING_TIME = LocalTime.of(10, 0);
    private static final LocalTime AFTERNOON_TIME = LocalTime.of(13, 0);
    private static final LocalTime EVENING_TIME = LocalTime.of(18, 0);
    private static final Pattern TIME_PATTERN =
            Pattern.compile("(?<!\\d)(?:saat\\s*)?(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?(?!\\d)");
    private static final Set<String> ALLOWED_SORTS =
            Set.of("recommended", "priceAsc", "priceDesc", "kmLimitDesc", "topRated", "mostReviewed");
    private static final Map<String, DayOfWeek> WEEKDAY_WORDS = weekdayWords();

    private final VehicleSearchAiClient aiClient;
    private final VehicleSearchHeuristicInterpreter heuristicInterpreter;
    private final VehicleRepository vehicleRepository;

    public PublicVehicleSearchInterpretResponse interpret(
            String query,
            LocalDateTime pickupDateTime,
            LocalDateTime returnDateTime,
            String requestLocation
    ) {
        String normalizedQuery = normalizeQuery(query);

        VehicleSearchAiResult fallback = heuristicInterpreter.interpret(normalizedQuery);
        VehicleSearchAiResult result;
        boolean providerFallback = false;
        try {
            result = heuristicInterpreter.merge(aiClient.interpret(normalizedQuery), fallback);
        } catch (Exception exception) {
            log.warn("AI marketplace interpretation failed: {}", exception.getClass().getSimpleName());
            result = fallback;
            providerFallback = true;
        }

        validateNumbers(result);
        List<String> warnings = sanitizeWarnings(result.warnings());
        List<String> inferences = new ArrayList<>();
        List<String> missingFields = sanitizeMissingFields(result.missingFields());
        if (providerFallback) {
            addWarning(warnings, "AI provider was unavailable, so supported search terms were interpreted locally.");
        }

        VehicleCategory explicitCategory = parseEnum(
                result.category(), VehicleCategory.class, "category", warnings
        );
        TransmissionType transmission = parseEnum(
                result.transmission(), TransmissionType.class, "transmission", warnings
        );
        FuelType fuelType = parseEnum(
                result.fuelType(), FuelType.class, "fuel type", warnings
        );
        PriceIntent priceIntent = parseEnum(
                result.priceIntent(), PriceIntent.class, "price intent", warnings
        );
        SegmentIntent segmentIntent = parseEnum(
                result.segmentIntent(), SegmentIntent.class, "segment intent", warnings
        );
        DateIntent aiDateIntent = parseEnum(
                result.dateIntent(), DateIntent.class, "date intent", warnings
        );
        DateResolution dateResolution = resolveDates(
                normalizedQuery,
                pickupDateTime,
                returnDateTime,
                aiDateIntent,
                inferences,
                warnings,
                missingFields
        );

        List<VehicleCategory> categories = resolveCategories(
                explicitCategory,
                segmentIntent,
                inferences
        );
        String location = firstNonBlank(normalizeText(result.location()), normalizeText(requestLocation));
        String brand = normalizeText(result.brand());
        String model = normalizeText(result.model());
        String sort = normalizeSort(result.sort(), warnings);
        BigDecimal minPrice = result.minDailyPrice();
        BigDecimal maxPrice = result.maxDailyPrice();

        if (priceIntent != null && minPrice == null && maxPrice == null) {
            PriceRange priceRange = resolveRelativePrice(
                    priceIntent,
                    pickupDateTime,
                    returnDateTime,
                    result.minDailyKmLimit(),
                    brand,
                    model,
                    categories,
                    transmission,
                    fuelType,
                    result.minSeats(),
                    location,
                    warnings
            );
            if (minPrice == null) {
                minPrice = priceRange.minPrice();
            }
            if (maxPrice == null) {
                maxPrice = priceRange.maxPrice();
            }
            if (priceRange.resolved()) {
                inferences.add(priceInference(priceIntent));
            }
        }

        var criteria = new PublicVehicleSearchCriteriaResponse(
                minPrice,
                maxPrice,
                result.minDailyKmLimit(),
                brand,
                model,
                categories,
                transmission,
                fuelType,
                result.minSeats(),
                location,
                sort
        );
        if (!heuristicInterpreter.hasCriteria(result) && !dateResolution.hasNaturalLanguageDate()) {
            addWarning(warnings, "No supported search preferences were found in the request.");
        }

        var dateCriteria = new PublicVehicleSearchDateCriteriaResponse(
                dateResolution.pickupDateTime(),
                dateResolution.returnDateTime()
        );
        var interpretation = new PublicVehicleSearchInterpretationResponse(
                priceIntent,
                segmentIntent,
                dateResolution.dateIntent()
        );
        String summary = result.summary() == null || result.summary().isBlank()
                ? defaultSummary(dateResolution, criteria)
                : truncate(result.summary().trim(), MAX_SUMMARY_LENGTH);

        return new PublicVehicleSearchInterpretResponse(
                criteria,
                dateCriteria,
                interpretation,
                List.copyOf(missingFields),
                summary,
                List.copyOf(inferences),
                List.copyOf(warnings)
        );
    }

    private List<VehicleCategory> resolveCategories(
            VehicleCategory explicitCategory,
            SegmentIntent segmentIntent,
            List<String> inferences
    ) {
        if (explicitCategory != null) {
            return List.of(explicitCategory);
        }
        if (segmentIntent == null) {
            return List.of();
        }

        List<VehicleCategory> categories = switch (segmentIntent) {
            case CITY -> List.of(VehicleCategory.ECONOMY, VehicleCategory.COMPACT);
            case MID_RANGE -> List.of(VehicleCategory.COMPACT, VehicleCategory.SEDAN);
            case FAMILY -> List.of(VehicleCategory.SEDAN, VehicleCategory.SUV, VehicleCategory.VAN);
            case SPACIOUS -> List.of(VehicleCategory.SUV, VehicleCategory.VAN);
            case PREMIUM -> List.of(VehicleCategory.LUXURY);
        };
        inferences.add(segmentInference(segmentIntent, categories));
        return categories;
    }

    private PriceRange resolveRelativePrice(
            PriceIntent intent,
            LocalDateTime pickupDateTime,
            LocalDateTime returnDateTime,
            Integer minDailyKmLimit,
            String brand,
            String model,
            List<VehicleCategory> categories,
            TransmissionType transmission,
            FuelType fuelType,
            Integer minSeats,
            String location,
            List<String> warnings
    ) {
        boolean categoriesEmpty = categories.isEmpty();
        List<String> queryCategories = categoriesEmpty
                ? Arrays.stream(VehicleCategory.values()).map(Enum::name).toList()
                : categories.stream().map(Enum::name).toList();

        VehiclePriceDistributionProjection distribution =
                vehicleRepository.calculateAvailablePriceDistribution(
                        pickupDateTime,
                        returnDateTime,
                        minDailyKmLimit,
                        normalizeForQuery(brand),
                        normalizeForQuery(model),
                        queryCategories,
                        categoriesEmpty,
                        transmission == null ? "" : transmission.name(),
                        fuelType == null ? "" : fuelType.name(),
                        minSeats,
                        normalizeForQuery(location)
                );

        if (distribution == null || distribution.getSampleCount() < MIN_PRICE_SAMPLE_SIZE) {
            addWarning(warnings, "Relative price could not be applied because too few matching vehicles are available.");
            return PriceRange.unresolved();
        }

        return switch (intent) {
            case BUDGET -> new PriceRange(null, money(distribution.getP30()), true);
            case AFFORDABLE -> new PriceRange(null, money(distribution.getP45()), true);
            case NOT_EXPENSIVE -> new PriceRange(null, money(distribution.getP60()), true);
            case MID_RANGE -> new PriceRange(money(distribution.getP30()), money(distribution.getP70()), true);
            case PREMIUM -> new PriceRange(money(distribution.getP75()), null, true);
        };
    }

    private String segmentInference(SegmentIntent intent, List<VehicleCategory> categories) {
        String values = categories.stream()
                .map(category -> title(category.name()))
                .reduce((left, right) -> left + " or " + right)
                .orElse("");
        return title(intent.name()) + " was interpreted as " + values + ".";
    }

    private String priceInference(PriceIntent intent) {
        return title(intent.name())
                + " was calculated from current prices of matching available vehicles.";
    }

    private DateResolution resolveDates(
            String query,
            LocalDateTime requestPickup,
            LocalDateTime requestReturn,
            DateIntent aiDateIntent,
            List<String> inferences,
            List<String> warnings,
            List<String> missingFields
    ) {
        ParsedDateQuery parsed = parseNaturalLanguageDates(query);
        LocalDateTime pickup = parsed.pickupDateTime() != null ? parsed.pickupDateTime() : requestPickup;
        LocalDateTime returned = parsed.returnDateTime() != null ? parsed.returnDateTime() : requestReturn;

        if (parsed.hasNaturalLanguageDate()) {
            pickup = parsed.pickupDateTime();
            returned = parsed.returnDateTime();
            if (parsed.inference() != null) {
                inferences.add(parsed.inference());
            }
        }

        if (pickup == null) {
            addMissing(missingFields, "pickupDateTime");
        }
        if (returned == null) {
            addMissing(missingFields, "returnDateTime");
        }
        if (pickup != null && pickup.isBefore(LocalDateTime.now())) {
            throw unavailable();
        }
        if (pickup != null && returned != null) {
            PublicVehicleService.validateDateRange(pickup, returned);
        } else if (parsed.hasNaturalLanguageDate()) {
            addWarning(warnings, "Choose pickup and return dates to search available vehicles.");
        }

        DateIntent dateIntent = parsed.dateIntent() != DateIntent.NONE
                ? parsed.dateIntent()
                : aiDateIntent == null ? DateIntent.NONE : aiDateIntent;
        return new DateResolution(pickup, returned, dateIntent, parsed.hasNaturalLanguageDate());
    }

    private ParsedDateQuery parseNaturalLanguageDates(String query) {
        String normalized = normalizeForNaturalLanguage(query);
        LocalDate today = LocalDate.now();
        List<LocalTime> times = extractTimes(normalized);

        if (containsAny(normalized, "hafta sonu", "weekend")) {
            LocalDate saturday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
            if (saturday.atTime(MORNING_TIME).isBefore(LocalDateTime.now())) {
                saturday = saturday.plusWeeks(1);
            }
            LocalDate sunday = saturday.plusDays(1);
            return new ParsedDateQuery(
                    saturday.atTime(times.isEmpty() ? MORNING_TIME : times.get(0)),
                    sunday.atTime(times.size() > 1 ? times.get(1) : EVENING_TIME),
                    DateIntent.WEEKEND,
                    true,
                    "Weekend was interpreted as Saturday morning to Sunday evening."
            );
        }

        List<ResolvedDateWord> dateWords = extractDateWords(normalized, today);
        if (dateWords.isEmpty()) {
            return ParsedDateQuery.none();
        }

        dateWords.sort(Comparator.comparingInt(ResolvedDateWord::position));
        LocalTime pickupTime = times.isEmpty() ? detectTimeOfDay(normalized, DEFAULT_PICKUP_TIME) : times.get(0);
        LocalDateTime pickup = dateWords.get(0).date().atTime(pickupTime);
        LocalDateTime returned = null;
        DateIntent intent = DateIntent.PICKUP_ONLY;

        if (dateWords.size() > 1) {
            LocalTime returnTime = times.size() > 1 ? times.get(1) : detectTimeOfDay(normalized, DEFAULT_RETURN_TIME);
            returned = dateWords.get(1).date().atTime(returnTime);
            if (!returned.isAfter(pickup)) {
                returned = returned.plusWeeks(1);
            }
            intent = DateIntent.DATE_RANGE;
        }

        String inference = returned == null
                ? "Pickup date was interpreted from the search request."
                : "Pickup and return dates were interpreted from the search request.";
        return new ParsedDateQuery(pickup, returned, intent, true, inference);
    }

    private List<ResolvedDateWord> extractDateWords(String query, LocalDate today) {
        List<ResolvedDateWord> dates = new ArrayList<>();
        addDateWord(query, "bugun", today, dates);
        addDateWord(query, "today", today, dates);
        addDateWord(query, "yarin", today.plusDays(1), dates);
        addDateWord(query, "tomorrow", today.plusDays(1), dates);
        WEEKDAY_WORDS.forEach((word, dayOfWeek) -> addDateWord(
                query,
                word,
                nextDateFor(dayOfWeek, today),
                dates
        ));
        return dates;
    }

    private void addDateWord(String query, String word, LocalDate date, List<ResolvedDateWord> dates) {
        int position = query.indexOf(word);
        if (position >= 0) {
            dates.add(new ResolvedDateWord(position, date));
        }
    }

    private LocalDate nextDateFor(DayOfWeek dayOfWeek, LocalDate today) {
        LocalDate date = today.with(TemporalAdjusters.nextOrSame(dayOfWeek));
        return date.atTime(DEFAULT_PICKUP_TIME).isBefore(LocalDateTime.now()) ? date.plusWeeks(1) : date;
    }

    private List<LocalTime> extractTimes(String query) {
        List<LocalTime> times = new ArrayList<>();
        Matcher matcher = TIME_PATTERN.matcher(query);
        while (matcher.find()) {
            int hour = Integer.parseInt(matcher.group(1));
            int minute = matcher.group(2) == null ? 0 : Integer.parseInt(matcher.group(2));
            String meridiem = matcher.group(3);
            if (meridiem != null && meridiem.equals("pm") && hour < 12) {
                hour += 12;
            }
            if (meridiem != null && meridiem.equals("am") && hour == 12) {
                hour = 0;
            }
            if (hour >= 0 && hour <= 23 && minute >= 0 && minute <= 59) {
                times.add(LocalTime.of(hour, minute));
            }
        }
        return times;
    }

    private LocalTime detectTimeOfDay(String query, LocalTime fallback) {
        if (containsAny(query, "aksam", "evening")) {
            return EVENING_TIME;
        }
        if (containsAny(query, "oglen", "afternoon")) {
            return AFTERNOON_TIME;
        }
        if (containsAny(query, "sabah", "morning")) {
            return MORNING_TIME;
        }
        return fallback;
    }

    private String defaultSummary(
            DateResolution dateResolution,
            PublicVehicleSearchCriteriaResponse criteria
    ) {
        if (dateResolution.hasNaturalLanguageDate()) {
            return dateResolution.returnDateTime() == null
                    ? "Date preferences were interpreted. Choose a return date to search available vehicles."
                    : "Dates and filters were extracted from your request.";
        }
        boolean hasFilter = criteria.minDailyPrice() != null
                || criteria.maxDailyPrice() != null
                || criteria.minDailyKmLimit() != null
                || criteria.brand() != null
                || criteria.model() != null
                || !criteria.categories().isEmpty()
                || criteria.transmission() != null
                || criteria.fuelType() != null
                || criteria.minSeats() != null
                || criteria.sort() != null;
        return hasFilter
                ? "Filters were extracted from your request."
                : "No filters were found in the search request.";
    }

    private String title(String value) {
        String normalized = value.toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }

    private BigDecimal money(Double value) {
        return value == null ? null : BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeQuery(String query) {
        if (query == null || query.isBlank()) {
            throw new BusinessException("Search query is required");
        }
        String normalized = query.trim();
        if (normalized.length() > MAX_QUERY_LENGTH) {
            throw new BusinessException("Search query cannot exceed 500 characters");
        }
        return normalized;
    }

    private void validateNumbers(VehicleSearchAiResult result) {
        if (isNegative(result.minDailyPrice()) || isNegative(result.maxDailyPrice())
                || result.minDailyKmLimit() != null && result.minDailyKmLimit() < 0
                || result.minSeats() != null && result.minSeats() < 1) {
            throw unavailable();
        }
        if (result.minDailyPrice() != null && result.maxDailyPrice() != null
                && result.minDailyPrice().compareTo(result.maxDailyPrice()) > 0) {
            throw unavailable();
        }
    }

    private boolean isNegative(BigDecimal value) {
        return value != null && value.signum() < 0;
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > MAX_TEXT_LENGTH) {
            throw unavailable();
        }
        return normalized;
    }

    private String firstNonBlank(String primary, String fallback) {
        return primary != null ? primary : fallback;
    }

    private String normalizeForQuery(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String normalizeForNaturalLanguage(String value) {
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

    private <E extends Enum<E>> E parseEnum(
            String value,
            Class<E> enumType,
            String fieldName,
            List<String> warnings
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(enumType, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            addWarning(warnings, "Ignored unsupported " + fieldName + " filter.");
            return null;
        }
    }

    private String normalizeSort(String value, List<String> warnings) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (!ALLOWED_SORTS.contains(normalized)) {
            addWarning(warnings, "Ignored unsupported sort option.");
            return null;
        }
        return normalized;
    }

    private List<String> sanitizeWarnings(List<String> source) {
        List<String> warnings = new ArrayList<>();
        if (source == null) {
            return warnings;
        }
        source.stream()
                .filter(item -> item != null && !item.isBlank())
                .map(String::trim)
                .map(item -> truncate(item, MAX_WARNING_LENGTH))
                .limit(MAX_WARNINGS)
                .forEach(warnings::add);
        return warnings;
    }

    private List<String> sanitizeMissingFields(List<String> source) {
        List<String> fields = new ArrayList<>();
        if (source == null) {
            return fields;
        }
        source.stream()
                .filter(item -> item != null && !item.isBlank())
                .map(String::trim)
                .filter(item -> item.equals("pickupDateTime") || item.equals("returnDateTime"))
                .forEach(item -> addMissing(fields, item));
        return fields;
    }

    private void addWarning(List<String> warnings, String warning) {
        if (warnings.size() < MAX_WARNINGS) {
            warnings.add(warning);
        }
    }

    private void addMissing(List<String> missingFields, String field) {
        if (!missingFields.contains(field)) {
            missingFields.add(field);
        }
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private ServiceUnavailableException unavailable() {
        return new ServiceUnavailableException(
                "AI vehicle search is temporarily unavailable. Manual filters are still available."
        );
    }

    private static Map<String, DayOfWeek> weekdayWords() {
        Map<String, DayOfWeek> words = new LinkedHashMap<>();
        words.put("pazartesi", DayOfWeek.MONDAY);
        words.put("monday", DayOfWeek.MONDAY);
        words.put("sali", DayOfWeek.TUESDAY);
        words.put("tuesday", DayOfWeek.TUESDAY);
        words.put("carsamba", DayOfWeek.WEDNESDAY);
        words.put("wednesday", DayOfWeek.WEDNESDAY);
        words.put("persembe", DayOfWeek.THURSDAY);
        words.put("thursday", DayOfWeek.THURSDAY);
        words.put("cuma", DayOfWeek.FRIDAY);
        words.put("friday", DayOfWeek.FRIDAY);
        words.put("cumartesi", DayOfWeek.SATURDAY);
        words.put("saturday", DayOfWeek.SATURDAY);
        words.put("pazar", DayOfWeek.SUNDAY);
        words.put("sunday", DayOfWeek.SUNDAY);
        return Map.copyOf(words);
    }

    private record PriceRange(BigDecimal minPrice, BigDecimal maxPrice, boolean resolved) {
        private static PriceRange unresolved() {
            return new PriceRange(null, null, false);
        }
    }

    private record DateResolution(
            LocalDateTime pickupDateTime,
            LocalDateTime returnDateTime,
            DateIntent dateIntent,
            boolean hasNaturalLanguageDate
    ) {
    }

    private record ParsedDateQuery(
            LocalDateTime pickupDateTime,
            LocalDateTime returnDateTime,
            DateIntent dateIntent,
            boolean hasNaturalLanguageDate,
            String inference
    ) {
        private static ParsedDateQuery none() {
            return new ParsedDateQuery(null, null, DateIntent.NONE, false, null);
        }
    }

    private record ResolvedDateWord(int position, LocalDate date) {
    }
}

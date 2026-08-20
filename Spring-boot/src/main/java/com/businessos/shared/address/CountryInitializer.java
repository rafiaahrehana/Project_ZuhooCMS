package com.businessos.shared.address;

import com.businessos.shared.exception.InternalServerException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.HashMap;

@Component
@RequiredArgsConstructor
public class CountryInitializer implements CommandLineRunner {

    private final CountryRepository countryRepository;

    @Override
    public void run(String... args) {
        try {
            if (countryRepository.count() == 0) {
                Map<String, String> countries = new HashMap<>();
                // South Asia
                countries.put("BD", "Bangladesh"); countries.put("IN", "India"); countries.put("PK", "Pakistan"); countries.put("LK", "Sri Lanka"); countries.put("NP", "Nepal"); countries.put("MV", "Maldives"); countries.put("AF", "Afghanistan");
                // Southeast & East Asia
                countries.put("CN", "China"); countries.put("JP", "Japan"); countries.put("KR", "South Korea"); countries.put("ID", "Indonesia"); countries.put("MY", "Malaysia"); countries.put("PH", "Philippines"); countries.put("SG", "Singapore"); countries.put("TH", "Thailand"); countries.put("VN", "Vietnam"); countries.put("TW", "Taiwan"); countries.put("MM", "Myanmar"); countries.put("KH", "Cambodia");
                // Americas
                countries.put("US", "United States"); countries.put("CA", "Canada"); countries.put("MX", "Mexico"); countries.put("BR", "Brazil"); countries.put("AR", "Argentina"); countries.put("CO", "Colombia"); countries.put("CL", "Chile"); countries.put("PE", "Peru"); countries.put("VE", "Venezuela"); countries.put("EC", "Ecuador"); countries.put("CU", "Cuba"); countries.put("BO", "Bolivia"); countries.put("PY", "Paraguay"); countries.put("UY", "Uruguay"); countries.put("CR", "Costa Rica"); countries.put("PA", "Panama"); countries.put("SV", "El Salvador"); countries.put("GT", "Guatemala"); countries.put("HN", "Honduras"); countries.put("DO", "Dominican Republic");
                // Europe
                countries.put("GB", "United Kingdom"); countries.put("DE", "Germany"); countries.put("FR", "France"); countries.put("IT", "Italy"); countries.put("ES", "Spain"); countries.put("RU", "Russia"); countries.put("UA", "Ukraine"); countries.put("PL", "Poland"); countries.put("NL", "Netherlands"); countries.put("BE", "Belgium"); countries.put("SE", "Sweden"); countries.put("CH", "Switzerland"); countries.put("AT", "Austria"); countries.put("PT", "Portugal"); countries.put("GR", "Greece"); countries.put("CZ", "Czech Republic"); countries.put("RO", "Romania"); countries.put("HU", "Hungary"); countries.put("IE", "Ireland"); countries.put("DK", "Denmark"); countries.put("FI", "Finland"); countries.put("NO", "Norway"); countries.put("TR", "Turkey"); countries.put("BY", "Belarus"); countries.put("RS", "Serbia"); countries.put("BG", "Bulgaria"); countries.put("SK", "Slovakia"); countries.put("HR", "Croatia"); countries.put("LT", "Lithuania"); countries.put("LV", "Latvia");
                // Middle East & North Africa
                countries.put("AE", "United Arab Emirates"); countries.put("SA", "Saudi Arabia"); countries.put("EG", "Egypt"); countries.put("IR", "Iran"); countries.put("IL", "Israel"); countries.put("IQ", "Iraq"); countries.put("DZ", "Algeria"); countries.put("MA", "Morocco"); countries.put("QA", "Qatar"); countries.put("KW", "Kuwait"); countries.put("OM", "Oman"); countries.put("LB", "Lebanon"); countries.put("JO", "Jordan"); countries.put("TN", "Tunisia");
                // Sub-Saharan Africa
                countries.put("ZA", "South Africa"); countries.put("NG", "Nigeria"); countries.put("KE", "Kenya"); countries.put("ET", "Ethiopia"); countries.put("GH", "Ghana"); countries.put("TZ", "Tanzania"); countries.put("UG", "Uganda"); countries.put("CI", "Ivory Coast"); countries.put("CM", "Cameroon"); countries.put("AO", "Angola"); countries.put("ZM", "Zambia"); countries.put("ZW", "Zimbabwe"); countries.put("SN", "Senegal"); countries.put("RW", "Rwanda");
                // Oceania
                countries.put("AU", "Australia"); countries.put("NZ", "New Zealand"); countries.put("FJ", "Fiji");

                for (Map.Entry<String, String> entry : countries.entrySet()) {
                    countryRepository.save(Country.builder()
                            .name(entry.getValue())
                            .code(entry.getKey())
                            .build());
                }
            }
        } catch (Exception ex) {
            throw new InternalServerException(
                    "Failed to initialize Country master data.",
                    ex
            );
        }
    }
}

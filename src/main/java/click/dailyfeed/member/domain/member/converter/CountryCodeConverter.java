package click.dailyfeed.member.domain.member.converter;

import click.dailyfeed.code.domain.member.member.type.data.CountryCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CountryCodeConverter implements AttributeConverter<CountryCode, String> {
    @Override
    public String convertToDatabaseColumn(CountryCode countryCode) {
        return countryCode != null ? countryCode.getCode() : null;
    }

    @Override
    public CountryCode convertToEntityAttribute(String code) {
        return code != null ? CountryCode.fromCode(code) : null;
    }
}

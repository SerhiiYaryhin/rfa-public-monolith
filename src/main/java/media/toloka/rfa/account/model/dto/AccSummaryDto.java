package media.toloka.rfa.account.model.dto;

public record AccSummaryDto(
        String uuid,
        Long id,
        Long acc,
        String accname,
        String operationcomment,
        Long totalValue
) {}
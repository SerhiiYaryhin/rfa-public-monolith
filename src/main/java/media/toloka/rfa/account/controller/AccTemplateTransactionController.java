package media.toloka.rfa.account.controller;

import lombok.RequiredArgsConstructor;
import media.toloka.rfa.account.model.AccTemplatePosting;
import media.toloka.rfa.account.model.AccTemplateTransaction;
import media.toloka.rfa.account.service.AccService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.beans.PropertyEditorSupport;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
//@RequestMapping("/acc/")
@RequiredArgsConstructor
public class AccTemplateTransactionController {

    @Autowired
    private AccService accService;

    @GetMapping("/acc/transactionlist")
    public String listTransactions(Model model) {

        List<AccTemplateTransaction> transactions = accService.findAllTransaction();
        model.addAttribute("transactions", transactions);
        return "/acc/transaction-list";
    }

    @GetMapping("/acc/template-form/{uuid}")
    public String createForm(
            @PathVariable String uuid,
            Model model) {

        AccTemplateTransaction transaction;
        transaction = accService.GetTemplareTransaction(uuid);
        if (transaction == null) {
            transaction = new AccTemplateTransaction();
            transaction.setEntry(new ArrayList<>());
        }
        model.addAttribute("transaction", transaction);
        return "/acc/template-form.html";
    }

    @PostMapping("/acc/templatetransactionsave/")
    public String save(@ModelAttribute("transaction") AccTemplateTransaction transaction) {
        for (AccTemplatePosting curentry : transaction.getEntry()) {
            curentry.setDebitacc( accService.GetAccByNumder(curentry.getDebit()) );
            curentry.setCreditacc( accService.GetAccByNumder(curentry.getCredit()) );
        }
        accService.SaveTransaction(transaction);
//        model.addAttribute("transaction", transaction);
        return "redirect:/acc/transactionlist";
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(BigDecimal.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                if (text == null || text.trim().isEmpty()) {
                    setValue(null);
                } else {
                    String normalized = text.replace(",", ".").replace(" ", "");
                    try {
                        setValue(new BigDecimal(normalized));
                    } catch (NumberFormatException e) {
                        setValue(null);
                    }
                }
            }

            @Override
            public String getAsText() {
                Object value = getValue();
                return (value != null) ? value.toString() : "";
            }
        });
    }
}



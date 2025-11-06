package com.kobe.warehouse.service.receipt.service;

import com.kobe.warehouse.domain.enumeration.ModePaimentCode;
import com.kobe.warehouse.service.dto.DepotExtensionSaleDTO;
import com.kobe.warehouse.service.dto.PaymentDTO;
import com.kobe.warehouse.service.dto.PaymentModeDTO;
import com.kobe.warehouse.service.dto.SaleDTO;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.dto.TvaEmbeded;
import com.kobe.warehouse.service.receipt.dto.CashSaleReceiptItem;
import com.kobe.warehouse.service.receipt.dto.HeaderFooterItem;
import com.kobe.warehouse.service.receipt.dto.SaleReceiptItem;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.service.utils.NumberUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public abstract class AbstractSaleReceiptService extends AbstractJava2DReceiptPrinterService {

    private final AppConfigurationService appConfigurationService;

    protected AbstractSaleReceiptService(AppConfigurationService appConfigurationService) {
        super(appConfigurationService);
        this.appConfigurationService = appConfigurationService;
    }

    protected abstract SaleDTO getSale();


    protected int getProductNameWidth() {
        return 22;
    }

    @Override
    protected abstract List<? extends SaleReceiptItem> getItems();


    protected int getAvoirCount() {
        return 0;
    }

    protected List<HeaderFooterItem> getOperateurInfos() {
        SaleDTO sale = getSale();
        Font font = PLAIN_FONT;
        List<HeaderFooterItem> headerItems = new ArrayList<>();
        headerItems.add(new HeaderFooterItem("TICKET: " + sale.getNumberTransaction(), 1, font));
        headerItems.add(new HeaderFooterItem("CASSIER(RE): " + sale.getCassier().getAbbrName(), 1, font));
        if (sale.getCassierId().compareTo(sale.getSellerId()) != 0) {
            headerItems.add(new HeaderFooterItem("VENDEUR(SE): " + sale.getSeller().getAbbrName(), 1, font));
        }
        return headerItems;
    }

    protected SaleReceiptItem fromSaleLine(SaleLineDTO saleLineDTO) {
        CashSaleReceiptItem item = new CashSaleReceiptItem();
        int productNameWidth = getProductNameWidth();
        var produitName = saleLineDTO.getProduitLibelle();
        item.setProduitName(produitName.length() > productNameWidth ? produitName.substring(0, productNameWidth) : produitName);
        item.setQuantity(NumberUtil.formatToString(saleLineDTO.getQuantityRequested()));
        item.setUnitPrice(NumberUtil.formatToString(saleLineDTO.getRegularUnitPrice()));
        item.setTotalPrice(NumberUtil.formatToString(saleLineDTO.getSalesAmount()));

        return item;
    }


    /**
     * Generate ESC/POS commands for thermal POS printer
     * Supports pagination and multiple copies
     *
     * @return byte array containing ESC/POS commands
     * @throws IOException if generation fails
     */
    public byte[] generateEscPosReceipt(boolean isEdit) throws IOException {
        magasin = appConfigurationService.getMagasin();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SaleDTO sale = getSale();
        List<? extends SaleReceiptItem> items = this.getItems();
        int numberOfCopies = isEdit ? 1 : getNumberOfCopies();

        try {
            // Print multiple copies
            for (int copyNum = 1; copyNum <= numberOfCopies; copyNum++) {
                if (copyNum > 1) {
                    // Add page break between copies
                    escPosFeedLines(out, 3);
                }

                // Pagination setup
                int linesPerPage = getMaximumLinesPerPage();
                int totalPages = (int) Math.ceil(items.size() / (double) linesPerPage);

                // Print each page
                for (int pageNum = 0; pageNum < totalPages; pageNum++) {
                    int startItemIndex = pageNum * linesPerPage;
                    int endItemIndex = Math.min(startItemIndex + linesPerPage, items.size());
                    boolean isLastPage = pageNum == totalPages - 1;

                    if (pageNum > 0) {
                        // Page break between pages
                        escPosFeedLines(out, 2);
                        escPosPrintSeparator(out, 48);
                        escPosFeedLines(out, 1);
                    }

                    // Print full header on every page
                    printEscPosHeader(out);

                    // Add page indicator on continuation pages
                    if (pageNum > 0) {
                        escPosSetAlignment(out, EscPosAlignment.CENTER);
                        escPosSetBold(out, true);
                        escPosPrintLine(out, String.format("--- Page %d/%d ---", pageNum + 1, totalPages));
                        escPosSetBold(out, false);
                        escPosSetAlignment(out, EscPosAlignment.LEFT);
                        escPosFeedLines(out, 1);
                    }

                    // Table header
                    escPosSetBold(out, true);
                    escPosPrintLine(out, String.format("%-3s %-24s %8s %10s", "QTE", "PRODUIT", "PU", "MONTANT"));
                    escPosSetBold(out, false);
                    escPosPrintSeparator(out, 48);

                    // Print items for this page
                    for (int i = startItemIndex; i < endItemIndex; i++) {
                        SaleReceiptItem item = items.get(i);
                        String quantity = item.getQuantity();
                        String productName = truncateString(item.getProduitName(), 24);
                        String unitPrice = item.getUnitPrice();
                        String totalPrice = item.getTotalPrice();

                        escPosPrintLine(out, String.format("%-3s %-24s %8s %10s", quantity, productName, unitPrice, totalPrice));
                    }

                    escPosPrintSeparator(out, 48);

                    // Print summary only on last page
                    if (isLastPage) {
                        printEscPosSummary(out, sale);
                    } else {
                        // Continuation indicator
                        escPosSetAlignment(out, EscPosAlignment.CENTER);
                        escPosSetBold(out, true);
                        escPosPrintLine(out, String.format(">>> Suite page %d >>>", pageNum + 2));
                        escPosSetBold(out, false);
                        escPosSetAlignment(out, EscPosAlignment.LEFT);
                    }
                }

                // Copy indicator
                if (numberOfCopies > 1) {
                    escPosFeedLines(out, 1);
                    escPosSetAlignment(out, EscPosAlignment.CENTER);
                    escPosPrintLine(out, String.format("*** COPIE %d/%d ***", copyNum, numberOfCopies));
                    escPosSetAlignment(out, EscPosAlignment.LEFT);
                }

                // Cut paper after each copy
                escPosFeedLines(out, 3);
                escPosCutPaper(out);
            }

            return out.toByteArray();
        } catch (Exception e) {
            throw new IOException("Failed to generate ESC/POS receipt: " + e.getMessage(), e);
        } finally {
            out.close();
        }
    }

    /**
     * Print ESC/POS header section (company info, customer info, etc.)
     */
    private void printEscPosHeader(ByteArrayOutputStream out) throws IOException {
        // Print common company header (name, address, phone, welcome message)
        printEscPosCompanyHeader(out);

        // Header items (customer info, operator, etc.)
        for (HeaderFooterItem headerItem : getHeaderItems()) {
            escPosPrintLine(out, headerItem.value());
        }
        escPosFeedLines(out, 1);

        // Insurance info (if applicable)
        String assuranceInfo = getAssuranceInfoText();
        if (assuranceInfo != null && !assuranceInfo.isEmpty()) {
            escPosPrintLine(out, assuranceInfo);
            escPosFeedLines(out, 1);
        }

        // Separator line
        escPosPrintSeparator(out, 48);
    }

    /**
     * Print ESC/POS summary section (totals, payments, taxes, footer)
     */
    private void printEscPosSummary(ByteArrayOutputStream out, SaleDTO sale) throws IOException {
        // Summary section
        escPosSetBold(out, true);
        escPosPrintLine(out, String.format("%-37s %10s", MONTANT_TTC, NumberUtil.formatToString(sale.getSalesAmount())));
        escPosSetBold(out, false);

        // Avoir section (if any)
        int avoirCount = getAvoirCount();
        if (avoirCount > 0) {
            escPosSetBold(out, true);
            escPosSetAlignment(out, EscPosAlignment.CENTER);
            escPosPrintLine(out, "Avoir( " + NumberUtil.formatToString(avoirCount) + " )");
            escPosSetAlignment(out, EscPosAlignment.LEFT);
            escPosSetBold(out, false);
        }

        // Discount (if any)
        if (sale.getDiscountAmount() > 0) {
            escPosPrintLine(out, String.format("%-37s %10s", REMISE, NumberUtil.formatToString(sale.getDiscountAmount())));
        }

        // Add spacing before total
        escPosFeedLines(out, 1);

        // Total to pay
        escPosSetBold(out, true);
        escPosSetTextSize(out, 2, 1); // Double width
        escPosPrintLine(out, String.format("%-13s %10s", TOTAL_A_PAYER, NumberUtil.formatToString(sale.getNetAmount())));
        escPosSetTextSize(out, 1, 1); // Normal size
        escPosSetBold(out, false);
        escPosFeedLines(out, 1);

        // Payment section
        if (!CollectionUtils.isEmpty(sale.getPayments())) {
            escPosSetBold(out, true);
            escPosSetAlignment(out, EscPosAlignment.CENTER);
            escPosPrintLine(out, REGLEMENT);
            escPosSetAlignment(out, EscPosAlignment.LEFT);
            escPosSetBold(out, false);
            escPosFeedLines(out, 1);
            int monnaie = 0;
            String amount;
            for (PaymentDTO payment : sale.getPayments()) {
                PaymentModeDTO paymentMode = payment.getPaymentMode();
                String libelle = paymentMode.getLibelle();
                if (paymentMode.getCode().equals(ModePaimentCode.CASH.name())) {
                    monnaie = payment.getMontantVerse() - sale.getNetAmount();
                    amount = NumberUtil.formatToString(payment.getMontantVerse());
                } else {
                    amount = NumberUtil.formatToString(payment.getPaidAmount());
                }

                escPosPrintLine(out, String.format("%-37s %10s", libelle, amount));
            }

            // Cash change (if any)
            if (monnaie > 0) {
                escPosPrintLine(out, String.format("%-37s %10s", MONTANT_RENDU, NumberUtil.formatToString(monnaie)));
            }

            // Remaining to pay (if any)
            if (sale.getRestToPay() > 0) {
                escPosSetBold(out, true);
                escPosPrintLine(out, String.format("%-37s %10s", RESTE_A_PAYER, NumberUtil.formatToString(sale.getRestToPay())));
                escPosSetBold(out, false);
            }

            escPosFeedLines(out, 1);
        } else {
            // Remaining to pay (if any)
            if (!(sale instanceof DepotExtensionSaleDTO) && sale.getRestToPay() > 0) {
                escPosSetBold(out, true);
                escPosPrintLine(out, String.format("%-37s %10s", RESTE_A_PAYER, NumberUtil.formatToString(sale.getRestToPay())));
                escPosSetBold(out, false);

                escPosFeedLines(out, 1);
            }


        }

        // Tax details (if any)
        if (!CollectionUtils.isEmpty(sale.getTvaEmbededs())) {
            escPosSetBold(out, true);
            escPosSetAlignment(out, EscPosAlignment.CENTER);
            escPosPrintLine(out, TVA);
            escPosSetAlignment(out, EscPosAlignment.LEFT);
            escPosSetBold(out, false);
            escPosFeedLines(out, 1);

            for (TvaEmbeded tva : sale.getTvaEmbededs()) {
                escPosPrintLine(out, String.format("%-37s %10s", "TVA " + tva.getTva() + "%", NumberUtil.formatToString(tva.getAmount())));
            }
            escPosFeedLines(out, 1);
        }

        // Footer items
        for (HeaderFooterItem footerItem : getFooterItems()) {
            escPosPrintLine(out, footerItem.value());
        }

        // Print common footer with sale timestamp
        printEscPosFooter(out, sale.getUpdatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
    }

    /**
     * Get insurance info as text (override in subclass if needed)
     */
    protected String getAssuranceInfoText() {
        return null; // Override in subclasses for insurance sales
    }

    /**
     * Estimate the height needed for a receipt page
     *
     * @param itemCount  number of items on this page
     * @param isLastPage whether this is the last page (includes summary)
     * @return estimated height in pixels
     */
    private int estimatePageHeight(int itemCount, boolean isLastPage) {
        int height = DEFAULT_MARGIN * 2; // Top and bottom margins

        // Header height (company info, welcome message, table header)
        height += DEFAULT_LINE_HEIGHT * 15; // Approximate header lines

        // Items height
        height += itemCount * DEFAULT_LINE_HEIGHT;

        // Summary height (only on last page)
        if (isLastPage) {
            // Summary section: total, payment, taxes, footer
            height += DEFAULT_LINE_HEIGHT * 20; // Approximate summary lines

            // Add extra space for payment modes (variable)
            SaleDTO sale = getSale();
            if (sale != null && sale.getPayments() != null) {
                height += sale.getPayments().size() * DEFAULT_LINE_HEIGHT;
            }

            // Add extra space for TVA details (variable)
            if (sale != null && sale.getTvaEmbededs() != null) {
                height += sale.getTvaEmbededs().size() * DEFAULT_LINE_HEIGHT;
            }
        }

        return height;
    }


}

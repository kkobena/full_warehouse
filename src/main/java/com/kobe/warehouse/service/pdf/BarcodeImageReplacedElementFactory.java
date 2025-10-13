package com.kobe.warehouse.service.pdf;


import java.awt.*;

import org.openpdf.text.Image;
import org.openpdf.text.pdf.BarcodeEAN;
import org.w3c.dom.Element;
import org.xhtmlrenderer.extend.FSImage;
import org.xhtmlrenderer.extend.ReplacedElement;
import org.xhtmlrenderer.extend.ReplacedElementFactory;
import org.xhtmlrenderer.extend.UserAgentCallback;
import org.xhtmlrenderer.layout.LayoutContext;
import org.xhtmlrenderer.pdf.ITextFSImage;
import org.xhtmlrenderer.pdf.ITextImageElement;
import org.xhtmlrenderer.render.BlockBox;
import org.xhtmlrenderer.simple.extend.FormSubmissionListener;

public class BarcodeImageReplacedElementFactory implements ReplacedElementFactory {

    private String barcodeData;

    public String getBarcodeData() {
        return barcodeData;
    }

    public BarcodeImageReplacedElementFactory setBarcodeData(String barcodeData) {
        this.barcodeData = barcodeData;
        return this;
    }

    @Override
    public ReplacedElement createReplacedElement(LayoutContext c, BlockBox box, UserAgentCallback uac, int cssWidth, int cssHeight) {
        Element e = box.getElement();
        if (e == null) {
            return null;
        }

        String nodeName = e.getNodeName();
        if (nodeName.equals("img")) {
            try {
                BarcodeEAN code = new BarcodeEAN();
                code.setCode(getBarcodeData());
                code.setCodeType(BarcodeEAN.EAN8);
                code.setGuardBars(true);
                java.awt.Image image = code.createAwtImage(Color.BLACK, Color.WHITE);
                FSImage fsImage = new ITextFSImage(Image.getInstance(image, Color.WHITE));
                if (cssWidth != -1 || cssHeight != -1) {
                    fsImage.scale(cssWidth, cssHeight);
                }

                return new ITextImageElement(fsImage);
            } catch (Throwable e1) {
                return null;
            }
        }

        return null;
    }

    @Override
    public void reset() {}

    @Override
    public void remove(Element e) {}

    @Override
    public void setFormSubmissionListener(FormSubmissionListener listener) {}
}

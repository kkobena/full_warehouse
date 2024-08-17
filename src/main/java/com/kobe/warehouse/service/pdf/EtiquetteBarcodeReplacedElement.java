package com.kobe.warehouse.service.pdf;

import com.lowagie.text.Image;
import com.lowagie.text.pdf.Barcode39;
import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

public class EtiquetteBarcodeReplacedElement implements ReplacedElementFactory {

    private final Logger log = LoggerFactory.getLogger(EtiquetteBarcodeReplacedElement.class);
    private final ReplacedElementFactory superFactory;
    private List<String> barcodesData = new ArrayList<>();

    public EtiquetteBarcodeReplacedElement(ReplacedElementFactory superFactory) {
        this.superFactory = superFactory;
    }

    public List<String> getBarcodesData() {
        return barcodesData;
    }

    public void setBarcodesData(List<String> barcodesData) {
        this.barcodesData = barcodesData;
    }

    @Override
    public ReplacedElement createReplacedElement(LayoutContext c, BlockBox box, UserAgentCallback uac, int cssWidth, int cssHeight) {
        Element e = box.getElement();
        if (e == null) {
            return null;
        }

        String id = e.getAttribute("id");
        FSImage fsImage;
        for (String barcodeData : getBarcodesData()) {
            if (id.equals(barcodeData)) {
                try {
                    fsImage = getBarcodeImage(barcodeData, cssWidth, cssHeight);

                    return new ITextImageElement(fsImage);
                } catch (IOException e1) {
                    log.error("Error creating barcode image", e1);
                    return null;
                }
            }
        }

        return superFactory.createReplacedElement(c, box, uac, cssWidth, cssHeight);
    }

    private FSImage getBarcodeImage(String barcodeData, int cssWidth, int cssHeight) throws IOException {
        Barcode39 code = new Barcode39();
        //  Element.ALIGN_CENTE
        code.setCode(barcodeData);
        //  code.setBaseline(10);
        //   code.set
        /* code.setCodeType(Barcode39.CODE128);
        code.setChecksumText(true);
        code.setBaseline(1f);
        code.setStartStopText(true);
        code.setGenerateChecksum(true);
        code.setAltText(barcodeData);
        code.setTextAlignment(com.lowagie.text.Element.ALIGN_CENTER);*/
        //        code.setGuardBars(true);
        java.awt.Image image = code.createAwtImage(Color.BLACK, Color.WHITE);
        FSImage fsImage = new ITextFSImage(Image.getInstance(image, Color.WHITE));
        if (cssWidth != -1 || cssHeight != -1) {
            fsImage.scale(cssWidth, cssHeight);
        }
        return fsImage;
    }

    //e EAN-13 barcode 6*19 mm
    @Override
    public void reset() {
        superFactory.reset();
    }

    @Override
    public void remove(Element e) {
        superFactory.remove(e);
    }

    @Override
    public void setFormSubmissionListener(FormSubmissionListener listener) {
        superFactory.setFormSubmissionListener(listener);
    }
}

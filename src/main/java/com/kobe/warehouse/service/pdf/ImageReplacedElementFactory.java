package com.kobe.warehouse.service.pdf;

import com.lowagie.text.BadElementException;
import com.lowagie.text.Image;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageReplacedElementFactory implements ReplacedElementFactory {


  @Override
  public ReplacedElement createReplacedElement(
      LayoutContext c, BlockBox box, UserAgentCallback uac, int cssWidth, int cssHeight) {

      Element e = box.getElement();
    if (e == null) {
      return null;
    }
    String nodeName = e.getNodeName();
    if (nodeName.equals("img")) {
      FSImage fsImage;
      try {
        fsImage = imageForPDF();
      } catch (BadElementException e1) {
        fsImage = null;
      } catch (IOException e1) {
        fsImage = null;
      }
      if (fsImage != null) {
        if (cssWidth != -1 || cssHeight != -1) {
          // System.out.println("scaling");
          fsImage.scale(cssWidth, cssHeight);
        } else {
          fsImage.scale(100, 100);
        }
        return new ITextImageElement(fsImage);
      }
    }
    return null;
  }

  protected FSImage imageForPDF()
      throws IOException, BadElementException {
      ResourceLoader resourceLoader = new DefaultResourceLoader();
    Resource resource = resourceLoader.getResource("classpath:templates/images/logo.png");
    FSImage fsImage;
    InputStream input = new FileInputStream(resource.getFile());
    final byte[] bytes = IOUtils.toByteArray(input);
    final Image image = Image.getInstance(bytes);
    fsImage = new ITextFSImage(image);
    return fsImage;
  }

  @Override
  public void reset() {}

  @Override
  public void remove(Element e) {}

  @Override
  public void setFormSubmissionListener(FormSubmissionListener listener) {}
}

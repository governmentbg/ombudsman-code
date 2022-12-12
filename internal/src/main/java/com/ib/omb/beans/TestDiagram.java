package com.ib.omb.beans;


import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.primefaces.model.diagram.Connection;
import org.primefaces.model.diagram.DefaultDiagramModel;
import org.primefaces.model.diagram.DiagramModel;
import org.primefaces.model.diagram.Element;
import org.primefaces.model.diagram.connector.FlowChartConnector;
import org.primefaces.model.diagram.endpoint.BlankEndPoint;
import org.primefaces.model.diagram.endpoint.EndPoint;
import org.primefaces.model.diagram.endpoint.EndPointAnchor;
import org.primefaces.model.diagram.overlay.ArrowOverlay;
import org.primefaces.model.diagram.overlay.LabelOverlay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.system.IndexUIbean;



@Named
@ViewScoped
public class TestDiagram extends IndexUIbean implements Serializable {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestDiagram.class);
	/**
	 * 
	 */
	private static final long serialVersionUID = 4985013225316883670L;
	
	
	private DefaultDiagramModel model;

    @PostConstruct
    public void init() {
        model = new DefaultDiagramModel();
        model.setMaxConnections(-1);

        FlowChartConnector connector = new FlowChartConnector();
        connector.setPaintStyle("{stroke:'#C7B097',strokeWidth:2}");
        model.setDefaultConnector(connector);

        Element start = new Element("Проверка за пълнота", "20em", "2em");
        start.addEndPoint(new BlankEndPoint(EndPointAnchor.TOP));
        start.addEndPoint(new BlankEndPoint(EndPointAnchor.BOTTOM));
//        start.addEndPoint(new BlankEndPoint(EndPointAnchor.LEFT));
        
        
        
        Element startIF = new Element("Коректно ли е попълнена?", "20em", "10em");
        startIF.addEndPoint(new BlankEndPoint(EndPointAnchor.TOP));
        startIF.addEndPoint(new BlankEndPoint(EndPointAnchor.BOTTOM));
//        startIF.addEndPoint(new BlankEndPoint(EndPointAnchor.RIGHT));
        startIF.setStyleClass("ui-diagram-success");
        
        
//        startIF.addEndPoint(new BlankEndPoint(EndPointAnchor.AUTO_DEFAULT));
        
        Element endIF = new Element("Прекратяване на проверка", "50em", "10em");
        endIF.addEndPoint(new BlankEndPoint(EndPointAnchor.TOP));
        endIF.addEndPoint(new BlankEndPoint(EndPointAnchor.BOTTOM));
//        endIF.addEndPoint(new BlankEndPoint(EndPointAnchor.RIGHT));
//        endIF.addEndPoint(new BlankEndPoint(EndPointAnchor.LEFT));
        
//        endIF.addEndPoint(new BlankEndPoint(EndPointAnchor.AUTO_DEFAULT));
        
        
        Element yes = new Element(" ", "20em", "18em");
        yes.addEndPoint(new BlankEndPoint(EndPointAnchor.TOP));
        yes.addEndPoint(new BlankEndPoint(EndPointAnchor.BOTTOM));
//        yes.addEndPoint(new BlankEndPoint(EndPointAnchor.RIGHT));
//        yes.addEndPoint(new BlankEndPoint(EndPointAnchor.LEFT));
        
//        yes.addEndPoint(new BlankEndPoint(EndPointAnchor.AUTO_DEFAULT));
        
        
        Element paralel1 = new Element("(Паралелен) подписване", "10em", "28em");
        paralel1.addEndPoint(new BlankEndPoint(EndPointAnchor.TOP));
        paralel1.addEndPoint(new BlankEndPoint(EndPointAnchor.BOTTOM));
//        paralel1.addEndPoint(new BlankEndPoint(EndPointAnchor.RIGHT));
//        paralel1.addEndPoint(new BlankEndPoint(EndPointAnchor.LEFT));
        
//        paralel1.addEndPoint(new BlankEndPoint(EndPointAnchor.AUTO_DEFAULT));
        
        Element paralel2 = new Element("(Паралелен) изпращане за сведение (Паралелен) изпращане за сведение (Паралелен) изпращане за сведение", "30em", "28em");
        paralel2.addEndPoint(new BlankEndPoint(EndPointAnchor.TOP));
        paralel2.addEndPoint(new BlankEndPoint(EndPointAnchor.BOTTOM));
//        paralel2.addEndPoint(new BlankEndPoint(EndPointAnchor.RIGHT));
//        paralel2.addEndPoint(new BlankEndPoint(EndPointAnchor.LEFT));
        
        
//        paralel2.addEndPoint(new BlankEndPoint(EndPointAnchor.AUTO_DEFAULT));
        
        Element union = new Element("Събирателен етап проверка за уведомление", "20em", "38em");
        union.addEndPoint(new BlankEndPoint(EndPointAnchor.TOP));
        union.addEndPoint(new BlankEndPoint(EndPointAnchor.BOTTOM));
//        union.addEndPoint(new BlankEndPoint(EndPointAnchor.RIGHT));
//        union.addEndPoint(new BlankEndPoint(EndPointAnchor.LEFT));
        
//        union.addEndPoint(new BlankEndPoint(EndPointAnchor.AUTO_DEFAULT));
        
        
        Element unionIf = new Element("Уведомен ли е?", "20em", "48em");
        unionIf.addEndPoint(new BlankEndPoint(EndPointAnchor.TOP));
        unionIf.addEndPoint(new BlankEndPoint(EndPointAnchor.BOTTOM));
//        unionIf.addEndPoint(new BlankEndPoint(EndPointAnchor.RIGHT));
//        unionIf.addEndPoint(new BlankEndPoint(EndPointAnchor.LEFT));
        unionIf.setStyleClass("ui-diagram-success");
        
//        unionIf.addEndPoint(new BlankEndPoint(EndPointAnchor.AUTO_DEFAULT));
        
        
        model.addElement(start);
        model.addElement(startIF);
        model.addElement(endIF);
        model.addElement(yes);
        model.addElement(paralel1);
        model.addElement(paralel2);
        model.addElement(union);
        model.addElement(unionIf);


//        model.connect(createConnection(start.getEndPoints().get(0), startIF.getEndPoints().get(0), null));
        
//        model.connect(createConnection(startIF.getEndPoints().get(2), endIF.getEndPoints().get(3), "Не"));
//        model.connect(createConnection(startIF.getEndPoints().get(1), yes.getEndPoints().get(0), "да"));
//        
//        model.connect(createConnection(yes.getEndPoints().get(1), paralel1.getEndPoints().get(0), null));
//        model.connect(createConnection(yes.getEndPoints().get(1), paralel2.getEndPoints().get(0), null));
//        
//        model.connect(createConnection(paralel1.getEndPoints().get(1), union.getEndPoints().get(0), null));
//        model.connect(createConnection(paralel2.getEndPoints().get(1), union.getEndPoints().get(0), null));
//        
//        model.connect(createConnection(union.getEndPoints().get(1), unionIf.getEndPoints().get(0), null));
//        
//        model.connect(createConnection(unionIf.getEndPoints().get(2), paralel2.getEndPoints().get(2), "Не"));
        
        model.connect(createConnection(start.getEndPoints().get(1), startIF.getEndPoints().get(0), null));
        
        model.connect(createConnection(startIF.getEndPoints().get(1), endIF.getEndPoints().get(0), "Не"));
      model.connect(createConnection(startIF.getEndPoints().get(1), yes.getEndPoints().get(0), "да"));
      
      model.connect(createConnection(yes.getEndPoints().get(1), paralel1.getEndPoints().get(0), null));
      model.connect(createConnection(yes.getEndPoints().get(1), paralel2.getEndPoints().get(0), null));
      
      model.connect(createConnection(paralel1.getEndPoints().get(1), union.getEndPoints().get(0), null));
      model.connect(createConnection(paralel2.getEndPoints().get(1), union.getEndPoints().get(0), null));
      
      model.connect(createConnection(union.getEndPoints().get(1), unionIf.getEndPoints().get(0), null));
      
      model.connect(createConnection(unionIf.getEndPoints().get(1), paralel2.getEndPoints().get(0), "Не"));
        

    }

    public DiagramModel getModel() {
        return model;
    }

    private Connection createConnection(EndPoint from, EndPoint to, String label) {
        Connection conn = new Connection(from, to);
        conn.getOverlays().add(new ArrowOverlay(20, 20, 1, 1));

        if (label != null) {
            conn.getOverlays().add(new LabelOverlay(label, "flow-label", 0.5));
        }

        return conn;
    }
	
}
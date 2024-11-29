package com.eaton.platform.core.workflows;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.eaton.platform.core.constants.AEMConstants;
import com.eaton.platform.core.util.WorkflowUtils;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Property;
import javax.jcr.Session;
import javax.jcr.Value;


/**
 * This workflow process decides which EPS density to use.
 */
@Component(service = WorkflowProcess.class,immediate = true,
    property = {
        AEMConstants.SERVICE_DESCRIPTION + "Eaton - EPS Density Decider",
        AEMConstants.SERVICE_VENDOR_EATON,
        AEMConstants.PROCESS_LABEL + "Eaton - EPS Density Decider"
    })
public class EPSDensityDecider implements WorkflowProcess {

    /**
     * The Constant LOG.
     */
    private static final Logger LOG = LoggerFactory.getLogger(EPSDensityDecider.class);

    /** The Constant CONTENT_TYPE_PROP. */
    private static final String CONTENT_TYPE_PROP = "jcr:content/metadata/xmp:eaton-content-type";

    /** The Constant CONTENT_TYPE_ILLUSTRATIONS. */
    private static final String CONTENT_TYPE_ILLUSTRATIONS = "eaton:resources/marketing-resources/illustrations";

    /** The Constant CONTENT_TYPE_PRODUCT_SPECIFICATION_GUIDES. */
    private static final String CONTENT_TYPE_PRODUCT_SPECIFICATION_GUIDES = "eaton:resources/technical-resources/product-specification-guides";

    /** The Constant CONTENT_TYPE_DRAWING. */
    private static final String CONTENT_TYPE_DRAWING = "eaton:resources/technical-resources/drawings";

    /** The Constant CONTENT_TYPE_WIRING_DIAGRAMS. */
    private static final String CONTENT_TYPE_WIRING_DIAGRAMS = "eaton:resources/technical-resources/wiring-diagrams";

    /** The Constant CONTENT_TYPE_TIME_CURRENT_CURVES. */
    private static final String CONTENT_TYPE_TIME_CURRENT_CURVES = "eaton:resources/technical-resources/time-current-curves";

    /** The Constant DENSITY_300. */
    private static final String DENSITY_300 = "300";

    /** The Constant DENSITY_1000. */
    private static final String DENSITY_1000 = "1000";

    /** The Constant DENSITY_1000. */
    private static final String DENSITY_DECISION = "DENSITY_DECISION";

    @Override
    public void execute(final WorkItem workItem, final WorkflowSession workflowSession, final MetaDataMap metaDataMap) throws WorkflowException {
        LOG.debug("EPSDensityDecider :: execute()");

        try {
            Session session = workflowSession.adaptTo(Session.class);
            Node root = session.getRootNode();
            Node payloadNode = WorkflowUtils.getPayloadNode(root, workItem);
            Node assetNode = WorkflowUtils.getAssetNode(payloadNode, LOG);
            if (assetNode == null) {
                LOG.error("Unable to find asset node for {}.", payloadNode.getPath());
                return;
            }

            StringBuilder sb = new StringBuilder();
            if (assetNode.hasProperty(CONTENT_TYPE_PROP)) {
                Property prop = assetNode.getProperty(CONTENT_TYPE_PROP);
                if (prop.isMultiple()) {
                    for (Value val : prop.getValues()) {
                        sb.append(val.getString());
                    }
                } else {
                    sb.append(prop.getString());
                }
            }

            String contentTypeValues = sb.toString();
            MetaDataMap wfMetadata = workItem.getWorkflowData().getMetaDataMap();
            if (contentTypeValues.contains(CONTENT_TYPE_DRAWING) || contentTypeValues.contains(CONTENT_TYPE_WIRING_DIAGRAMS) || contentTypeValues.contains(CONTENT_TYPE_TIME_CURRENT_CURVES)) {
                wfMetadata.put(DENSITY_DECISION, DENSITY_1000);
                LOG.debug("Setting {} to {}", DENSITY_DECISION, DENSITY_1000);
            } else if (contentTypeValues.contains(CONTENT_TYPE_ILLUSTRATIONS) || contentTypeValues.contains(CONTENT_TYPE_PRODUCT_SPECIFICATION_GUIDES)) {
                wfMetadata.put(DENSITY_DECISION, DENSITY_300);
                LOG.debug("Setting {} to {}", DENSITY_DECISION, DENSITY_300);
            }
        } catch (RepositoryException ex) {
            LOG.error("An exception occurred in the EPS Density Decider\n{}", ex);
        }
    }
}

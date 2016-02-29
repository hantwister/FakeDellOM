/*
    FakeDellOM
    Copyright (C) 2016 Harrison Neal, hneal@imalazybastard.com

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package fakedell;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 *
 * @author Harrison
 */
public class XMLResponder extends HttpServlet {

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest req, HttpServletResponse rsp)
            throws ServletException, IOException {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setValidating(true);
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();

            String reqXml = IOUtils.toString(req.getReader());

            Document reqDoc = db.parse(IOUtils.toInputStream(reqXml));

            Node bodyNode = reqDoc.getElementsByTagNameNS("http://www.w3.org/2003/05/soap-envelope", "Body").item(0);

            Node firstBodyNode = bodyNode.getFirstChild();
            String ns = firstBodyNode.getNamespaceURI();
            String ln = firstBodyNode.getLocalName();

            if (ns.equalsIgnoreCase("http://schemas.dmtf.org/wbem/wsman/identity/1/wsmanidentity.xsd")
                    && ln.equalsIgnoreCase("Identify")) {
                rsp.setContentType("application/soap+xml;charset=UTF-8");
                rsp.getWriter().append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsmid=\"http://schemas.dmtf.org/wbem/wsman/identity/1/wsmanidentity.xsd\"><s:Header/><s:Body><wsmid:IdentifyResponse><wsmid:ProtocolVersion>http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd</wsmid:ProtocolVersion><wsmid:ProductVendor>Fake Dell Open Manage Server</wsmid:ProductVendor><wsmid:ProductVersion>1.0</wsmid:ProductVersion></wsmid:IdentifyResponse></s:Body></s:Envelope>");
            } else if (ns.equalsIgnoreCase("http://schemas.dmtf.org/wbem/wscim/1/cim-schema/2/DCIM_OEM_DataAccessModule")
                    && ln.equalsIgnoreCase("SendCmd_INPUT")) {
                String cmd = firstBodyNode.getChildNodes().item(0).getTextContent();

                String addressingNs = "http://schemas.xmlsoap.org/ws/2004/08/addressing";

                String incomingId = reqDoc.getElementsByTagNameNS(addressingNs, "MessageID").item(0).getTextContent();
                String senderName = reqDoc.getElementsByTagNameNS(addressingNs, "Address").item(0).getTextContent();
                String outgoingId = "";

                for (char c : "nnnnnnnn-nnnn-nnnn-nnnn-nnnnnnnnnnnn".toCharArray()) {
                    switch (c) {
                        case 'n':
                            outgoingId += Integer.toHexString((int) (16 * Math.random()));
                            break;
                        default:
                            outgoingId += c;
                    }
                }

                String xmlBase = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:n1=\"http://schemas.dmtf.org/wbem/wscim/1/cim-schema/2/DCIM_OEM_DataAccessModule\"><s:Header><wsa:To> </wsa:To><wsa:RelatesTo> </wsa:RelatesTo><wsa:MessageID> </wsa:MessageID></s:Header><s:Body><n1:SendCmd_OUTPUT><n1:ResultCode>0</n1:ResultCode><n1:ReturnValue> </n1:ReturnValue></n1:SendCmd_OUTPUT></s:Body></s:Envelope>";

                Document rspDoc = db.parse(IOUtils.toInputStream(xmlBase));

                rspDoc.getElementsByTagNameNS(addressingNs, "To").item(0).setTextContent(senderName);
                rspDoc.getElementsByTagNameNS(addressingNs, "RelatesTo").item(0).setTextContent(incomingId);
                rspDoc.getElementsByTagNameNS(addressingNs, "MessageID").item(0).setTextContent(outgoingId);

                String n1Ns = "http://schemas.dmtf.org/wbem/wscim/1/cim-schema/2/DCIM_OEM_DataAccessModule";

                Node errorCodeNode = rspDoc.getElementsByTagNameNS(n1Ns, "ResultCode").item(0);
                Node returnStringNode = rspDoc.getElementsByTagNameNS(n1Ns, "ReturnValue").item(0);

                if (cmd.startsWith("__00omacmd=getuserrightsonly ")) {
                    returnStringNode.setTextContent("<SMStatus>0</SMStatus><UserRightsMask>" + (7 + (7 << 16)) + "</UserRightsMask>");
                } else if (cmd.startsWith("__00omacmd=getaboutinfo ")) {
                    returnStringNode.setTextContent("<ProductVersion>6.0.3</ProductVersion>");
                } else {
                    throw new IllegalArgumentException("Unrecognized input");
                }

                rsp.setContentType("application/soap+xml;charset=UTF-8");
                TransformerFactory tf = TransformerFactory.newInstance();
                Transformer t = tf.newTransformer();
                t.transform(new DOMSource(rspDoc), new StreamResult(rsp.getWriter()));
            } else {
                throw new IllegalArgumentException("Unrecognized input");
            }
        } catch (Throwable t) {
            rsp.setContentType("text/plain");
            t.printStackTrace(rsp.getWriter());
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}

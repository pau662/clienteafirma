package es.gob.afirma.standalone.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.TextAttribute;
import java.util.Map;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import es.gob.afirma.core.signers.AOSigner;
import es.gob.afirma.core.signers.AOSignerFactory;
import es.gob.afirma.core.ui.AOUIFactory;
import es.gob.afirma.signers.pades.AOPDFSigner;
import es.gob.afirma.standalone.LookAndFeelManager;
import es.gob.afirma.standalone.SimpleAfirmaMessages;
import es.gob.afirma.standalone.configurator.common.PreferencesManager;
import es.gob.afirma.standalone.ui.SignOperationConfig.CryptoOperation;
import es.gob.afirma.standalone.ui.preferences.AgePolicy;
import es.gob.afirma.standalone.ui.preferences.PreferencesDialog;

public class SignatureConfigInfoPanel extends JPanel {

    /** Serial Id. */
	private static final long serialVersionUID = -2242593726948948111L;

	private JCheckBox pdfVisible = null;

    private JCheckBox pdfStamp = null;

    private String accesibleDescription;
    
    private JLabel signFormatLabel;
    
    private JPanel attributesPanel;
    
    private JPanel signOptionsPanel;
    
    private SignPanelFilePanel signPanelFile;

	public SignatureConfigInfoPanel(final SignOperationConfig signConfig, final Color bgColor, final SignPanelFilePanel signPanel) {
		this.signPanelFile = signPanel;
		createUI(signConfig, bgColor);
	}

	public SignatureConfigInfoPanel() {
		
	}

	private SignatureConfigInfoPanel createUI(final SignOperationConfig signConfig, final Color bgColor) {

		//Eliminamos los componentes anteriores si existieran por si se esta actualizando el panel
		removeAll();
		
		if (!LookAndFeelManager.WINDOWS_HIGH_CONTRAST) {
			setBackground(bgColor);
		}

		// Formato de firma
		this.signFormatLabel = new JLabel(
				SimpleAfirmaMessages.getString("SignPanel.103", signConfig.getSignatureFormatName())); //$NON-NLS-1$
		this.accesibleDescription += SimpleAfirmaMessages.getString("SignPanel.103", signConfig.getSignatureFormatName()); //$NON-NLS-1$

		// Resumen de atributos
		final JLabel attrLabel = new JLabel(
				SimpleAfirmaMessages.getString("SignPanel.143")); //$NON-NLS-1$
		this.accesibleDescription += SimpleAfirmaMessages.getString("SignPanel.143"); //$NON-NLS-1$
		this.attributesPanel = createAttributesPanel(signConfig);

		// Opciones de firma
		this.signOptionsPanel = createOptionsPanel(signConfig, bgColor);
		
		// Opciones de formato
		final JPanel formatOptionsPanel = createFormatOptionsPanel(signConfig, bgColor);

		// Agregamos los elementos al panel
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		add(Box.createRigidArea(new Dimension(0, 4)));
		add(formatOptionsPanel);
		
        if (this.attributesPanel != null) {
        	if (!LookAndFeelManager.WINDOWS_HIGH_CONTRAST) {
        		this.attributesPanel.setBackground(bgColor);
        	}
        	add(Box.createRigidArea(new Dimension(0, 4)));
        	add(attrLabel);
        	add(Box.createRigidArea(new Dimension(0, 4)));
        	add(this.attributesPanel);
        }
        if (this.signOptionsPanel != null) {
        	if (!LookAndFeelManager.WINDOWS_HIGH_CONTRAST) {
        		this.signOptionsPanel.setBackground(bgColor);
        	}
        	add(Box.createRigidArea(new Dimension(0, 4)));
        	add(this.signOptionsPanel);
        }
        return this;
	}
	
	private JPanel createAttributesPanel(final SignOperationConfig config) {

		String policyId = null;
		String roles = null;
		boolean definedPlace = false;
		if (config.getExtraParams() != null) {
			final Properties exParams = config.getExtraParams();
			policyId = exParams.getProperty("policyIdentifier"); //$NON-NLS-1$
			roles = exParams.getProperty("signerClaimedRoles"); //$NON-NLS-1$
			definedPlace = exParams.containsKey("signatureProductionStreetAddress") || //$NON-NLS-1$
					exParams.containsKey("signatureProductionCity") || //$NON-NLS-1$
					exParams.containsKey("signatureProductionProvince") || //$NON-NLS-1$
					exParams.containsKey("signatureProductionPostalCode") || //$NON-NLS-1$
					exParams.containsKey("signatureProductionCountry"); //$NON-NLS-1$
		}

		// Para mostrar este apartado debe haber declarada una politica
		// de firma o un rol de firmante
		if (policyId == null && roles == null && !definedPlace) {
			return null;
		}
		
		final JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		// Resumen de politica de firma
		if (policyId != null) {
			String policyDescription = null;
			if (AgePolicy.isAGEPolicy18(policyId)) {
				policyDescription = SimpleAfirmaMessages.getString("PreferencesPanel.25"); //$NON-NLS-1$
			} else if (AgePolicy.isAGEPolicy19(policyId)) {
				policyDescription = SimpleAfirmaMessages.getString("PreferencesPanel.73"); //$NON-NLS-1$
			} else {
				policyDescription = SimpleAfirmaMessages.getString("PreferencesPanel.26"); //$NON-NLS-1$
			}
			this.accesibleDescription += policyDescription;
			panel.add(new JLabel(" - " + policyDescription)); //$NON-NLS-1$
			panel.add(Box.createRigidArea(new Dimension(0, 4)));
		}

		// Resumen del lugar de firma
		if (definedPlace) {
			final String placeDescription = SimpleAfirmaMessages.getString("SignPanel.149"); //$NON-NLS-1$
			this.accesibleDescription += placeDescription;
			panel.add(new JLabel(" - " + placeDescription)); //$NON-NLS-1$
			panel.add(Box.createRigidArea(new Dimension(0, 4)));
		}

		// Resumen de roles
		if (roles != null) {
			panel.add(new JLabel(" - " + SimpleAfirmaMessages.getString("SignPanel.144", roles))); //$NON-NLS-1$ //$NON-NLS-2$
			this.accesibleDescription += SimpleAfirmaMessages.getString("SignPanel.144", roles); //$NON-NLS-1$
			panel.add(Box.createRigidArea(new Dimension(0, 4)));
		}

		// Enlace para ver todos los atributos
		panel.add(createAttributesHiperlink(SimpleAfirmaMessages.getString("SignPanel.146"), config)); //$NON-NLS-1$
		this.accesibleDescription += SimpleAfirmaMessages.getString("SignPanel.146"); //$NON-NLS-1$

		return panel;
	}

	private JLabel createAttributesHiperlink(final String text, final SignOperationConfig signConfig) {

		final JLabel hlLabel = new JLabel(" - " + text); //$NON-NLS-1$

		hlLabel.setFocusable(true);
		hlLabel.setForeground(LookAndFeelManager.WINDOWS_HIGH_CONTRAST ? Color.yellow : Color.blue);
    	final Font font = hlLabel.getFont();
    	final Map attributes = font.getAttributes();
    	attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
    	hlLabel.setFont(font.deriveFont(attributes));

    	hlLabel.getAccessibleContext().setAccessibleName(SimpleAfirmaMessages.getString("SignDataPanel.46") + " " + text);  //$NON-NLS-1$//$NON-NLS-2$
    	this.accesibleDescription += SimpleAfirmaMessages.getString("SignDataPanel.46") + " " + text;  //$NON-NLS-1$//$NON-NLS-2$

		final SignatureAttributesListener linkListener = new SignatureAttributesListener(signConfig);
		hlLabel.addMouseListener(linkListener);
		hlLabel.addFocusListener(linkListener);
		hlLabel.addKeyListener(linkListener);

		return hlLabel;
	}

	private JLabel createChangeFormatHiperlink(final String text, final SignOperationConfig signConfig, final Color bgColor) {

		final JLabel hlLabel = new JLabel(text); 

		hlLabel.setFocusable(true);
		hlLabel.setForeground(LookAndFeelManager.WINDOWS_HIGH_CONTRAST ? Color.yellow : Color.blue);
    	final Font font = hlLabel.getFont();
    	final Map attributes = font.getAttributes();
    	attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
    	hlLabel.setFont(font.deriveFont(attributes));

    	hlLabel.getAccessibleContext().setAccessibleName(SimpleAfirmaMessages.getString("SignDataPanel.46") + " " + text);  //$NON-NLS-1$//$NON-NLS-2$
    	this.accesibleDescription += SimpleAfirmaMessages.getString("SignDataPanel.46") + " " + text;  //$NON-NLS-1$//$NON-NLS-2$

		final ChangeFormatListener linkListener = new ChangeFormatListener(this, signConfig, bgColor, this.signPanelFile);
		hlLabel.addMouseListener(linkListener);
		hlLabel.addFocusListener(linkListener);
		hlLabel.addKeyListener(linkListener);

		return hlLabel;
	}

	private JPanel createOptionsPanel(final SignOperationConfig config, final Color bgColor) {

		final JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		final AOSigner signer = config.getSigner();
		final CryptoOperation cop = config.getCryptoOperation();
		final FileType fileType = config.getFileType();

		// Agrega opciones adicionales de firma PDF
        if (signer instanceof AOPDFSigner) {

        	// Check para la generacion de firma visible PDF
            this.pdfVisible = new JCheckBox(
        		SimpleAfirmaMessages.getString("SignPanel.44"), //$NON-NLS-1$
        		PreferencesManager.getBoolean(PreferencesManager.PREFERENCE_PADES_VISIBLE)
        	);
            this.accesibleDescription += SimpleAfirmaMessages.getString("SignPanel.44"); //$NON-NLS-1$
            if (!LookAndFeelManager.WINDOWS_HIGH_CONTRAST) {
            	this.pdfVisible.setBackground(bgColor);
            }
            this.pdfVisible.setMnemonic('H');
            panel.add(Box.createRigidArea(new Dimension(0, 4)));
            panel.add(this.pdfVisible);

            // Check para agregar una imagen al PDF antes de la firma
            this.pdfStamp = new JCheckBox(
            		SimpleAfirmaMessages.getString("SignPanel.120"), //$NON-NLS-1$
            		PreferencesManager.getBoolean(PreferencesManager.PREFERENCE_PADES_STAMP)
            	);
            this.accesibleDescription += SimpleAfirmaMessages.getString("SignPanel.120"); //$NON-NLS-1$
            if (!LookAndFeelManager.WINDOWS_HIGH_CONTRAST) {
            	this.pdfStamp.setBackground(bgColor);
            }
            this.pdfStamp.setMnemonic('S');
            panel.add(Box.createRigidArea(new Dimension(0, 4)));
            panel.add(this.pdfStamp);

            if (cop == CryptoOperation.COSIGN) {
            	this.pdfStamp.setSelected(false);
            	this.pdfStamp.setEnabled(false);
            	panel.add(Box.createRigidArea(new Dimension(0, 4)));
            	panel.add(new JLabel(SimpleAfirmaMessages.getString("SignPanel.121"))); //$NON-NLS-1$
            	this.accesibleDescription += SimpleAfirmaMessages.getString("SignPanel.121"); //$NON-NLS-1$
            }
        }

        // Agrega boton de opciones avanzadas de multifirma CAdES y XAdES
        if (fileType == FileType.SIGN_CADES || fileType == FileType.SIGN_XADES) {
        	final JButton avanzado = new JButton(SimpleAfirmaMessages.getString("SignPanel.119")); //$NON-NLS-1$
        	this.accesibleDescription += SimpleAfirmaMessages.getString("SignPanel.119"); //$NON-NLS-1$

        	panel.add(Box.createRigidArea(new Dimension(0, 6)));
        	avanzado.setMnemonic('a');
        	avanzado.addActionListener(
    			ae -> {
    				if(fileType == FileType.SIGN_CADES) {
    					PreferencesDialog.show(null, true, 2);
    				}
    				else {
    					PreferencesDialog.show(null, true, 3);
    				}
    			}
        	);
        	panel.add(avanzado);
        }

        return panel;
	}
	
	private JPanel createFormatOptionsPanel(final SignOperationConfig config, final Color bgColor) {

		final JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		
		if (!LookAndFeelManager.WINDOWS_HIGH_CONTRAST) {
			panel.setBackground(bgColor);
		}

		panel.add(this.signFormatLabel);
		panel.add(Box.createRigidArea(new Dimension(4, 0)));
		if (CryptoOperation.SIGN.equals(config.getCryptoOperation())) {
			panel.add(createChangeFormatHiperlink(SimpleAfirmaMessages.getString("SignPanel.157"), config, bgColor)); //$NON-NLS-1$
			panel.add(Box.createRigidArea(new Dimension(4, 0)));
		}
		
        return panel;
	}

	public boolean isPdfVisibleSignatureSelected() {
		return this.pdfVisible != null && this.pdfVisible.isSelected();
	}

	public boolean isPdfStampSignatureSelected() {
		return this.pdfStamp != null && this.pdfStamp.isSelected();
	}

	public String getAccesibleDescription() {
		return this.accesibleDescription;
	}
	
	public JLabel getSignFormatLabel() {
		return this.signFormatLabel;
	}

	public JPanel getAttributesPanel() {
		return this.attributesPanel;
	}

	public JPanel getSignOptionsPanel() {
		return this.signOptionsPanel;
	}

	public void setAttributesPanel(final JPanel attributesPanel) {
		this.attributesPanel = attributesPanel;
	}

	public void setSignOptionsPanel(final JPanel signOptionsPanel) {
		this.signOptionsPanel = signOptionsPanel;
	}

	static class SignatureAttributesListener implements MouseListener, FocusListener, KeyListener {

		private final SignOperationConfig signConfig;

		public SignatureAttributesListener(final SignOperationConfig signConfig) {
			this.signConfig = signConfig;
		}

		@Override
		public void mouseReleased(final MouseEvent e) { /* No hacemos nada */ }

		@Override
		public void mousePressed(final MouseEvent e) { /* No hacemos nada */ }

		@Override
		public void mouseExited(final MouseEvent e) {
			e.getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}

		@Override
		public void mouseEntered(final MouseEvent e) {
			e.getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		}

		@Override
		public void mouseClicked(final MouseEvent e) {
			executeAction((Component) e.getSource(), this.signConfig);
		}

		@Override
		public void focusGained(final FocusEvent e) {
			((JComponent) e.getSource()).setBorder(BorderFactory.createDashedBorder(null, 1, 1));
		}

		@Override
		public void focusLost(final FocusEvent e) {
			((JComponent) e.getSource()).setBorder(null);
		}

		private static void executeAction(final Component component, final SignOperationConfig config) {
			final JDialog dialog = SignatureAttributesDialog.newInstance(component, config);
    		dialog.getAccessibleContext().setAccessibleDescription(SignatureAttributesDialog.getAccessibleDescription());
			dialog.setVisible(true);
		}

		@Override
		public void keyTyped(final KeyEvent e) { /* No hacemos nada */ }

		@Override
		public void keyPressed(final KeyEvent e) { /* No hacemos nada */ }

		@Override
		public void keyReleased(final KeyEvent e) {
			if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE) {
				executeAction((Component) e.getSource(), this.signConfig);
			}
		}
	}
	
	 class ChangeFormatListener implements MouseListener, FocusListener, KeyListener {

		private final SignOperationConfig signConfig;
		private final SignatureConfigInfoPanel panel;
		private final SignPanelFilePanel signPanel;
		private final Color bgColor;

		public ChangeFormatListener(final SignatureConfigInfoPanel panel, final SignOperationConfig signConfig, final Color bgColor, final SignPanelFilePanel signPanel) {
			this.panel = panel;
			this.signConfig = signConfig;
			this.bgColor = bgColor;
			this.signPanel = signPanel;
		}

		@Override
		public void mouseReleased(final MouseEvent e) { /* No hacemos nada */ }

		@Override
		public void mousePressed(final MouseEvent e) { /* No hacemos nada */ }

		@Override
		public void mouseExited(final MouseEvent e) {
			e.getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}

		@Override
		public void mouseEntered(final MouseEvent e) {
			e.getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		}

		@Override
		public void mouseClicked(final MouseEvent e) {
			executeAction(this.signConfig);
		}

		@Override
		public void focusGained(final FocusEvent e) {
			((JComponent) e.getSource()).setBorder(BorderFactory.createDashedBorder(null, 1, 1));
		}

		@Override
		public void focusLost(final FocusEvent e) {
			((JComponent) e.getSource()).setBorder(null);
		}

		private void executeAction(final SignOperationConfig config) {
			final ChangeFormatPanel changeFormatPanel = new ChangeFormatPanel(config);
			this.panel.getAccessibleContext().setAccessibleDescription(ChangeFormatPanel.getAccessibleDescription());
			if (AOUIFactory.showConfirmDialog(
					null,
					changeFormatPanel,
					SimpleAfirmaMessages.getString("ChangeFormatDialog.0"), //$NON-NLS-1$
					AOUIFactory.OK_CANCEL_OPTION,
					AOUIFactory.PLAIN_MESSAGE
			) == AOUIFactory.YES_OPTION) {
				String selectedFormat = (String) changeFormatPanel.getUsedCombo().getSelectedItem();
	
				final String recommendedStr = " " + SimpleAfirmaMessages.getString("ChangeFormatDialog.2");  //$NON-NLS-1$//$NON-NLS-2$
				
				if (selectedFormat.contains(recommendedStr)) {
					selectedFormat = selectedFormat.replace(recommendedStr, ""); //$NON-NLS-1$
				}
				
				final AOSigner signer = AOSignerFactory.getSigner(selectedFormat);
				this.signConfig.setSigner(signer);
				final Properties extraParams = ExtraParamsHelper.loadExtraParamsForSigner(signer);
				
				this.signConfig.setExtraParams(extraParams);
				this.signConfig.setSignatureFormatName(SignPanel.getSignatureName(config.getSigner()));

				final SignatureConfigInfoPanel updatedPanel = createUI(this.signConfig, this.bgColor);
				this.signPanel.setConfigInfoPanel(updatedPanel);
				this.signPanel.updateUI();
			}
		}

		@Override
		public void keyTyped(final KeyEvent e) { /* No hacemos nada */ }

		@Override
		public void keyPressed(final KeyEvent e) { /* No hacemos nada */ }

		@Override
		public void keyReleased(final KeyEvent e) {
			if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE) {
				executeAction(this.signConfig);
			}
		}
	}
}

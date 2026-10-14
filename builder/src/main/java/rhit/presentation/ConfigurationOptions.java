package rhit.presentation;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import rhit.domain.BuilderData;
import rhit.domain.PropertiesLoader;
import rhit.domain.TemplateType;

class ConfigurationOptions extends SwingGui {
    private static final int BORDER_SIZE = 5;
    private static final int GRID_PADDING = 2;
    private static final String MBL_KEY = "mbl";
    private static final Pattern SKILL_CODE = Pattern.compile("^[A-Z]+\\d+");
    private static final int COL_CODE = 0;
    private static final int COL_PACKAGE = 1;
    private static final int COL_NAME = 2;
    private static final int COL_PROBLEMS = 3;
    private static final int COL_REQUIRED = 4;
    private static final int COL_MANUAL = 5;

    private final JFrame frame;
    private final Map<String, String> formValues;
    private final Map<String, Boolean> didVisitArrayEditor;
    private JPanel panel;
    private JCheckBox mblCheckBox;
    private DefaultTableModel mblModel;

    ConfigurationOptions() {
        this.frame = InterfaceUtils.getFrame();
        super.verifyFrame(frame);
        this.formValues = new HashMap<>();
        this.didVisitArrayEditor = new HashMap<>();
        BuilderData.parseConfigFile();
    }

    void show() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(
                BorderFactory.createEmptyBorder(BORDER_SIZE, BORDER_SIZE, BORDER_SIZE, BORDER_SIZE));

        generateFormPanel(formPanel);

        JButton continueButton = new JButton(PropertiesLoader.get("continueButton"));
        continueButton.addActionListener(e -> handleContinue());

        panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(formPanel);
        if (BuilderData.getTemplateType() == TemplateType.AUTO) {
            panel.add(createMblPanel(), BorderLayout.NORTH);
        }
        panel.add(continueButton, BorderLayout.SOUTH);
        frame.add(panel);

        InterfaceUtils.updateFrame();
    }

    private JPanel createMblPanel() {
        mblCheckBox = new JCheckBox(PropertiesLoader.get("mblModeLabel"));
        mblModel = new DefaultTableModel(new Object[] { "Skill", "Package", "Name", "Problems",
                "Required", "Manual" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column >= COL_NAME;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                return column == COL_PROBLEMS || column == COL_REQUIRED ? Integer.class
                        : column == COL_MANUAL ? Boolean.class : String.class;
            }
        };
        populateMblModel();

        JScrollPane tablePane = new JScrollPane(new JTable(mblModel));
        tablePane.setVisible(false);
        mblCheckBox.addActionListener(e -> {
            tablePane.setVisible(mblCheckBox.isSelected());
            InterfaceUtils.updateFrame();
        });

        JPanel mblPanel = new JPanel(new BorderLayout());
        mblPanel.setBorder(
                BorderFactory.createEmptyBorder(BORDER_SIZE, BORDER_SIZE, BORDER_SIZE, BORDER_SIZE));
        mblPanel.add(mblCheckBox, BorderLayout.NORTH);
        mblPanel.add(tablePane, BorderLayout.CENTER);
        return mblPanel;
    }

    private void populateMblModel() {
        for (Entry<String, Integer> entry : InterfaceUtils
                .countTestClassesByPackage(BuilderData.getTemplateFiles()).entrySet()) {
            Matcher matcher = SKILL_CODE.matcher(entry.getKey());
            String code = matcher.find() ? matcher.group() : entry.getKey();
            boolean manual = !entry.getKey().endsWith("Test");
            mblModel.addRow(new Object[] { code, entry.getKey(), "", entry.getValue(),
                    manual ? 0 : entry.getValue() - 1, manual });
        }
    }

    /**
     * Validates the MBL table and, if MBL mode is on, adds the "mbl" block to the
     * configuration.
     *
     * @return false if the table is invalid and the user has been notified
     */
    @SuppressWarnings("unchecked")
    private boolean applyMblOptions() {
        JSONObject configOptions = BuilderData.getConfigOptions();
        configOptions.remove(MBL_KEY);
        if (mblCheckBox == null || !mblCheckBox.isSelected()) {
            return true;
        }
        if (mblModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(frame, PropertiesLoader.get("mblNoSkillsError"));
            return false;
        }

        JSONArray skills = new JSONArray();
        for (int row = 0; row < mblModel.getRowCount(); row++) {
            boolean manual = (Boolean) mblModel.getValueAt(row, COL_MANUAL);
            int problems = (Integer) mblModel.getValueAt(row, COL_PROBLEMS);
            int required = (Integer) mblModel.getValueAt(row, COL_REQUIRED);
            String code = (String) mblModel.getValueAt(row, COL_CODE);
            if (!manual && (required < 1 || required > problems)) {
                JOptionPane.showMessageDialog(frame,
                        String.format(PropertiesLoader.get("mblRequiredError"), code, problems));
                return false;
            }
            JSONObject skill = new JSONObject();
            skill.put("code", code);
            skill.put("package", mblModel.getValueAt(row, COL_PACKAGE));
            skill.put("name", mblModel.getValueAt(row, COL_NAME));
            skill.put("required", manual ? 0 : required);
            skill.put("problems", problems);
            skill.put("manual", manual);
            skills.add(skill);
        }
        JSONObject mbl = new JSONObject();
        mbl.put("enabled", true);
        mbl.put("skills", skills);
        configOptions.put(MBL_KEY, mbl);

        Object classes = configOptions.get("classes");
        if (classes instanceof JSONArray) {
            for (Object cls : (JSONArray) classes) {
                ((JSONObject) cls).put("weight", -1.0);
            }
        }
        return true;
    }

    private void generateFormPanel(JPanel formPanel) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets.set(GRID_PADDING, GRID_PADDING, GRID_PADDING, GRID_PADDING);

        JSONObject configOptions = BuilderData.getConfigOptions();
        iterativeFormPanel(configOptions, formPanel, gbc);
    }

    @SuppressWarnings("unchecked")
    private void iterativeFormPanel(JSONObject configOptions, JPanel formPanel,
            GridBagConstraints gbc) {
        configOptions.forEach((key, value) -> {
            if (MBL_KEY.equals(key)) {
                return;
            }
            if (value instanceof JSONObject) {
                iterativeFormPanel((JSONObject) value, formPanel, gbc);
                return;
            }
            gbc.gridx = 0;
            String keyLabel = String.join(" ", key.toString().split("[-_]"));
            keyLabel = keyLabel.isEmpty() ? "" : keyLabel.substring(0, 1).toUpperCase() + keyLabel.substring(1);
            formPanel.add(new JLabel(keyLabel + ": "), gbc);
            gbc.gridx = 1;
            if (value instanceof JSONArray) {
                addArrayEditOption(key.toString(), (JSONArray) value, formPanel, gbc);
            } else {
                addTextFieldOption(key, value, configOptions, formPanel, gbc);
            }
            gbc.gridy++;
        });
    }

    private void addArrayEditOption(String label, JSONArray array, JPanel formPanel,
            GridBagConstraints gbc) {
        if (label.equals("classes")) {
            InterfaceUtils.createClassObjectsFromPackages(array, BuilderData.getTemplateFiles());
        }
        JButton button = new JButton(PropertiesLoader.get("editButton"));
        didVisitArrayEditor.put(label, false);
        button.addActionListener(e -> {
            didVisitArrayEditor.put(label, true);
            displayArrayEditor(array);
        });
        formPanel.add(button, gbc);
    }

    private void displayArrayEditor(JSONArray array) {
        ArrayEditorDialog dialog = new ArrayEditorDialog(frame, array);
        dialog.setVisible(true);
    }

    private void addTextFieldOption(Object label, Object value, JSONObject configOptions,
            JPanel formPanel, GridBagConstraints gbc) {
        JTextField textField = new JTextField(value.toString());
        formValues.put(label.toString(), value.toString());
        textField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                handleChange();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                handleChange();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                handleChange();
            }

            @SuppressWarnings("unchecked")
            private void handleChange() {
                Object newValue = textField.getText();
                formValues.put(label.toString(), (String) newValue);
                if (((String) newValue).isEmpty()) {
                    return;
                }
                if (value instanceof Long) {
                    newValue = Long.parseLong((String) newValue);
                } else if (value instanceof Boolean) {
                    newValue = Boolean.parseBoolean((String) newValue);
                }
                configOptions.put(label, newValue);
            }
        });
        formPanel.add(textField, gbc);
    }

    protected void handleContinue() {
        if (formValues.values().stream().anyMatch(String::isEmpty)) {
            JOptionPane.showMessageDialog(frame, PropertiesLoader.get("emptyFieldError"));
            return;
        }
        if (didVisitArrayEditor.values().stream().anyMatch(visited -> !visited)) {
            JOptionPane.showMessageDialog(frame, PropertiesLoader.get("arrayEditorError"));
            return;
        }
        if (!applyMblOptions()) {
            return;
        }
        InterfaceUtils.hideFrame(panel);
        SwingGui.setVisibleFrame(new BuildProgress());
        SwingGui.showFrame();
    }
}

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

public class LPSolver {

    public static void main(String[] args) {
        // Create the frame
        JFrame frame = new JFrame("Linear Program Solver");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);

        // Create the panel to hold the components
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        frame.add(panel);

        // Create a panel for the results
        JTabbedPane resultsTabbed = new JTabbedPane();

        // Create table model for objective function
        String[] objectiveColumnNames = {"Variable", "Coefficient"};
        Object[][] objectiveData = {{"x1", 0}, {"x2", 0}};
        DefaultTableModel objectiveTableModel = new DefaultTableModel(objectiveData, objectiveColumnNames);
        JTable objectiveTable = new JTable(objectiveTableModel);
        JScrollPane scrollPaneObjective = new JScrollPane(objectiveTable);
        scrollPaneObjective.setBorder(BorderFactory.createTitledBorder("Objective Function"));
        panel.add(scrollPaneObjective, BorderLayout.NORTH);

        // Create table model for constraints
        String[] constraintColumnNames = {"Variable", "x1", "x2", "RHS"};
        Object[][] constraintData = {{"Constraint 1", 0, 0, 0}, {"Constraint 2", 0, 0, 0}};
        DefaultTableModel constraintTableModel = new DefaultTableModel(constraintData, constraintColumnNames);
        JTable constraintTable = new JTable(constraintTableModel);
        JScrollPane scrollPaneConstraints = new JScrollPane(constraintTable);
        scrollPaneConstraints.setBorder(BorderFactory.createTitledBorder("Constraints"));
        panel.add(scrollPaneConstraints, BorderLayout.CENTER);

        // Create buttons for adding rows and columns
        JPanel buttonPanel = new JPanel();
        panel.add(buttonPanel, BorderLayout.SOUTH);

        // Init checkbox for either maximalization or minimalization & set default to checked
        JCheckBox addMaxminCheckbox = new JCheckBox("Maximalization"); addMaxminCheckbox.setSelected(true);

        // init the result UI plane
        JPanel resultsPanel = new JPanel();
        resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));
        JScrollPane resultScroll = new JScrollPane(resultsPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        // Reset button
        JButton addResetButton = new JButton("Reset");
        addResetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Clear the objective function table
                for (int i = 0; i < objectiveTableModel.getRowCount(); i++) {
                    for (int j = 1; j < objectiveTableModel.getColumnCount(); j++) {
                        objectiveTableModel.setValueAt(0, i, j);
                    }
                }

                // Clear the constraints table
                for (int i = 0; i < constraintTableModel.getRowCount(); i++) {
                    for (int j = 1; j < constraintTableModel.getColumnCount(); j++) {
                        constraintTableModel.setValueAt(0, i, j);
                    }
                }

                // Clear the results panel
                resultsPanel.removeAll();
                resultsPanel.revalidate();
                resultsPanel.repaint();
            }
        });
        buttonPanel.add(addResetButton);

        JButton addRowButton = new JButton("Add Constraint");
        addRowButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Add a new constraint row with default values
                int columnCount = constraintTableModel.getColumnCount();
                Object[] newRow = new Object[columnCount];
                newRow[0] = "Constraint " + (constraintTableModel.getRowCount() + 1); // Set constraint name
                for (int i = 1; i < columnCount; i++) {
                    newRow[i] = 0; // Initialize all coefficients and RHS to 0
                }
                constraintTableModel.addRow(newRow);
            }
        });
        buttonPanel.add(addRowButton);

        JButton addColumnButton = new JButton("Add Variable");
        addColumnButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String variableName = "x" + (objectiveTableModel.getRowCount() + 1);
                objectiveTableModel.addRow(new Object[]{variableName, 0});

                // Insert a new column before the RHS column
                int rhsColumnIndex = constraintTableModel.findColumn("RHS");
                int newColumnIndex = constraintTableModel.getColumnCount() - 1;

                // Add the new column to the table model
                constraintTableModel.addColumn("x" + (newColumnIndex));

                // Move the new column to the position before the RHS column
                constraintTable.moveColumn(rhsColumnIndex, newColumnIndex + 1);

                // Initialize the new column values to 0
                for (int i = 0; i < constraintTableModel.getRowCount(); i++) {
                    constraintTableModel.setValueAt(0, i, newColumnIndex + 1);
                }
            }
        });
        buttonPanel.add(addColumnButton);

        JButton addSolveButton = new JButton("Solve");
        addSolveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int rows =  constraintTableModel.getRowCount();

                // add the slack variables
                for (int i = 0; i < rows; i++) {
                    constraintTableModel.addColumn("s" + (i + 1));
                    objectiveTableModel.addRow(new Object[]{"s" + (i + 1), 0});
                    for (int j = 0; j < constraintTableModel.getRowCount(); j++) {
                        if (i == j) {
                            constraintTableModel.setValueAt(1, j, constraintTableModel.findColumn("s" + (i + 1)));
                        } else {
                            constraintTableModel.setValueAt(0, j, constraintTableModel.findColumn("s" + (i + 1)));
                        }
                    }
                }

                int cols = constraintTableModel.getColumnCount();;
                constraintTable.moveColumn(constraintTableModel.findColumn("RHS"), cols - 1);
                // get the correct column order
                int[] columnOrder = new int[cols];
                for (int col = 0; col < cols; col++) {
                    columnOrder[col] = constraintTable.convertColumnIndexToModel(col);
                }

//                Vector<Vector> rowData = constraintTableModel.getDataVector();
                float[][] data = new float[rows + 1][cols - 1];

//              add the data from the table to a data type we can handle in the solver
                for (int i = 0; i < rows; i++) {
                    for (int j = 1; j < cols; j++) {
                        int modelIndex = columnOrder[j];
                        data[i][j - 1] = Float.parseFloat(constraintTableModel.getValueAt(i, modelIndex).toString());
                    }
                }

                // append the objective function from the other table to this one
                objectiveTableModel.addRow(new Object[]{"RHS", 0});
                Vector<Vector> objectiveRowData = objectiveTableModel.getDataVector();

                for (int i = 0; i < objectiveTableModel.getRowCount(); i++) {
                    if (addMaxminCheckbox.isSelected()) {
                        data[rows][i] = -1f * Float.parseFloat(objectiveRowData.get(i).get(1).toString());
                    } else {
                        data[rows][i] = Float.parseFloat(objectiveRowData.get(i).get(1).toString());
                    }
                }

                resultsTabbed.add("Solution", resultScroll);

                //initialize simplex
                Simplex solver = new Simplex(data.length -1, data[0].length - 1);
                solver.fillTable(data);
                Simplex.WrapperResults pivot = null;
                Simplex.ERROR status = Simplex.ERROR.NOT_OPTIMAL;
                int pivotColumn;

                int itterationCounter = 0;
                int limit = 50;
                while(status == Simplex.ERROR.NOT_OPTIMAL) {
                    // generate the columns name
                    float[][] output = solver.getTable();
                    int numCols = output[0].length;
                    String[] columns = new String[numCols + 1];
                    columns[0] = "Name:";
                    for (int i = 0; i < ((cols - 2) / 2) ; i++) {
                        columns[i + 1] = "x" + (i + 1);
                    }
                    for (int i = 0; i < rows; i++) {
                        columns[i + ((cols - 2) / 2) + 1] = "s" + (i + 1);
                    }
                    columns[numCols] = "RHS";

                    // Create table model for iteration result
                    DefaultTableModel iterationTableModel = new DefaultTableModel(columns, 0);
                    for (int rowIndex = 0; rowIndex < output.length; rowIndex++) {
                        float[] row = output[rowIndex];
                        Object[] values = new Object[row.length + 1]; // +1 for the "Name" column
                        if (rowIndex < output.length - 1) {
                            values[0] = "Constraint " + (rowIndex + 1);
                        } else {
                            values[0] = "Objective Function";
                        }
                        for (int i = 0; i < row.length; i++) {
                            values[i + 1] = row[i]; // +1 to shift right for the "Name" column
                        }
                        iterationTableModel.addRow(values);
                    }
                    JTable outputTable = new JTable(iterationTableModel);
                    JScrollPane outputScroll = new JScrollPane(outputTable);
                    if (pivot != null) {
                        pivotColumn = pivot.getPivotColumn();
                    } else {
                        pivotColumn = 0;
                    }
                    outputScroll.setBorder(BorderFactory.createTitledBorder("Result Pivot " + itterationCounter + " | Pivot Column " + pivotColumn));
                    resultsPanel.add(outputScroll);
                    resultsPanel.revalidate();

                    // Pivot
                    pivot = solver.compute();
                    status = pivot.getError();

                    itterationCounter++;
                    if (itterationCounter > limit) { // set as a hard limit to prevent inf loop
                        break;
                    }
                }
                JLabel outputLabel = new JLabel("None");
                outputLabel.setFont(new Font("Ariel", Font.BOLD, 25));
                if (status == Simplex.ERROR.UNBOUNDED) {
                    outputLabel.setText("The LP provided was unbounded and could not be solved");
                } else if (status == Simplex.ERROR.IS_OPTIMAL) {
                    outputLabel.setText("The LP was solved!");
                } else {
                    outputLabel.setText("Something went wrong, maybe the itteration limit of " + limit + " was reached!");
                }
                resultsPanel.add(outputLabel, BorderLayout.CENTER);
            }
        });
        buttonPanel.add(addSolveButton);
        buttonPanel.add(addMaxminCheckbox);

        resultsTabbed.add("Main", panel);

        frame.add(resultsTabbed);
        frame.setVisible(true);
    }
}

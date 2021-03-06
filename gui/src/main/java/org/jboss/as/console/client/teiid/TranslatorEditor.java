/*
 * Copyright Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags and
 * the COPYRIGHT.txt file distributed with this work.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.console.client.teiid;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.MultipleToOneLayout;
import org.jboss.as.console.client.teiid.model.Translator;
import org.jboss.ballroom.client.widgets.forms.CheckBoxItem;
import org.jboss.ballroom.client.widgets.forms.ComboBoxItem;
import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.ballroom.client.widgets.forms.NumberBoxItem;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.ballroom.client.widgets.window.Feedback;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SingleSelectionModel;

public class TranslatorEditor implements Persistable<Translator> {
    
    private DefaultCellTable<Translator> table;
    private ListDataProvider<Translator> dataProvider;
    
    private TeiidModelForm<Translator> formCommon;
    private SubsystemPresenter presenter;
    private DefaultWindow window;
    
    public TranslatorEditor(SubsystemPresenter presenter) {
        this.presenter = presenter;
    }    

    public Widget asWidget() {
        
        ClickHandler addClickHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                launchTranslatorWizard();
            }
        };

        ClickHandler deleteClickHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                final Translator selection = getCurrentSelection();
                Feedback.confirm(
                        Console.MESSAGES.deleteTitle("Translator"),
                        Console.MESSAGES.deleteConfirm("Translator " + selection.getName()),
                        new Feedback.ConfirmationHandler() {
                            @Override
                            public void onConfirmation(boolean isConfirmed) {
                                if (isConfirmed) {
                                    delete(selection);
                                }
                            }
                        });
            }
        };
        
        ToolButton addBtn = new ToolButton(Console.CONSTANTS.common_label_add()); 
        addBtn.addClickHandler(addClickHandler);
        ToolButton deleteBtn = new ToolButton(Console.CONSTANTS.common_label_delete());
        deleteBtn.addClickHandler(deleteClickHandler);
        
        ToolStrip topLevelTools = new ToolStrip();
        topLevelTools.addToolButtonRight(addBtn);
        topLevelTools.addToolButtonRight(deleteBtn);
        
        this.table = new DefaultCellTable<Translator>(5, new ProvidesKey<Translator>() {
                    @Override
                    public Object getKey(Translator item) {
                        return item.getName();
                    }
                });

        this.dataProvider = new ListDataProvider<Translator>();
        this.dataProvider.addDataDisplay(this.table);        
        
        TextColumn<Translator> nameColumn = new TextColumn<Translator>() {
            @Override
            public String getValue(Translator record) {
                return record.getName();
            }
        };

        TextColumn<Translator> protocolColumn = new TextColumn<Translator>() {
            @Override
            public String getValue(Translator record) {
                return  record.getModuleName();
            }
        };
        
        TextColumn<Translator> slotColumn = new TextColumn<Translator>() {
            @Override
            public String getValue(Translator record) {
                return  record.getSlot();
            }
        };
        
        this.table.addColumn(nameColumn, "Name");
        this.table.addColumn(protocolColumn, "Module Name");
        this.table.addColumn(slotColumn, "Slot");
              
        this.formCommon = new TeiidModelForm<Translator>(Translator.class,
                this, buildCommonFormItems().toArray(new FormItem<?>[2]));
        
        this.formCommon.setTable(this.table);
        
        MultipleToOneLayout layoutBuilder = new MultipleToOneLayout()
                .setPlain(true)
                .setTitle("Translators")
                .setHeadline("Translators")
                .setDescription(new SafeHtmlBuilder().appendEscaped("Translator provides a mechanism to "
                        + "to integrate data from various source systems").toSafeHtml())
                .setMaster(Console.MESSAGES.available("Translators"), table)
                .setMasterTools(topLevelTools.asWidget())
                .addDetail("Common", this.formCommon.asWidget());

        return layoutBuilder.build();

    }
    
    static List<FormItem<?>> buildCommonFormItems(){
        TextBoxItem name = new TextBoxItem("name", "Name", true);
        TextBoxItem moduleName = new TextBoxItem("moduleName", "Module Name", true);
        TextBoxItem slot = new TextBoxItem("slot", "Slot", true);
        return Arrays.asList(name, moduleName, slot);
    }
    
    public void setTranslators(List<Translator> translators) {
        this.dataProvider.setList(translators);
        this.table.selectDefaultEntity();
    }
    
    private Translator getCurrentSelection() {
        return ((SingleSelectionModel<Translator>) this.table.getSelectionModel()).getSelectedObject();
    }
    
    public void launchTranslatorWizard() {
        try {
            this.window = new DefaultWindow(Console.MESSAGES.createTitle("Translator"));
            this.window.setWidth(480);
            this.window.setHeight(360);

            TranslatorWizard wizard = new TranslatorWizard(this);
            
            this.window.trapWidget(wizard.asWidget());

            this.window.setGlassEnabled(true);
            this.window.center();
        } catch (Exception e) {
            Console.error("Error while starting the wizard for new Translator");
        }
    }    

    @Override
    public void save(Translator translator, Map<String, Object> changeset) {
        this.presenter.saveTranslator(translator, changeset);
    }

    private void delete(Translator selection) {
        this.presenter.deleteTranslator(selection);
    }    
    
    public void closeNewTranslatorWizard() {
        this.window.hide();
    }

    public void createNewTranslator(Translator translator) {
        this.window.hide();
        this.presenter.createTranslator(translator);
    }    
}

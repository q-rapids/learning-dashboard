var serverUrl = sessionStorage.getItem("serverUrl");
var classifiersTree;
var previousSelectionId;

function buildTree() {
        $.ajax({
            url: '../api/metrics/list',
            type: "GET",
        success: function (data) {
            classifiersTree = data;
            var classifier1List = document.getElementById('MetricList');
            var classifier1Listson = document.createElement('li');
            classifier1Listson.setAttribute("style", "height:400px; overflow: scroll;");
            classifier1Listson.classList.add("list-group");
            classifier1List.innerHTML = "";
            classifier1Listson.innerHTML = "";
            for (var i=0; i<data.length; i++) {
                var classifier1 = document.createElement('li');
                classifier1.classList.add("list-group-item");
                classifier1.classList.add("Classifier");
                classifier1.setAttribute("id", "classifier" + data[i]);
                //classifier1.setAttribute('style', 'background-color: #ffffff;');
                classifier1.setAttribute("data-toggle", "collapse");
                classifier1.setAttribute("data-target", ("#sonsOf" + data[i]));
                classifier1.addEventListener("click", loadMetricsCategories.bind(null, data[i]));

                var icon_c1 = document.createElement('img');
                icon_c1.classList.add("icons");
                icon_c1.setAttribute("src", "/icons/folder.png");
                icon_c1.setAttribute("style", "margin-right: 5px;");
                classifier1.appendChild(icon_c1);
                var text_c1 = document.createElement('p');
                text_c1.appendChild(document.createTextNode(data[i]));
                classifier1.appendChild(text_c1);
                classifier1Listson.appendChild(classifier1);

            }
            classifier1List.appendChild(classifier1Listson);
            //document.getElementById('MetricList').appendChild(classifier1List);

        }
    });
}


$("#SICategoriesButton").click(function () {
    selectElement($(this));
    $("#SICategories").show();
    $("#FactorsCategories").hide();
    $("#MetricsCategories").hide();
    document.getElementById('MetricsForm').innerHTML = "";
    document.getElementById('MetricList').innerHTML = "";
    //addButtonBehaviour();
});

$("#FactorsCategoriesButton").click(function () {
    selectElement($(this));
    $("#SICategories").hide();
    $("#FactorsCategories").show();
    $("#MetricsCategories").hide();
    document.getElementById('MetricsForm').innerHTML = "";
    document.getElementById('MetricList').innerHTML = "";
    //addButtonBehaviour();
});

$("#MetricsCategoriesButton").click(function () {
    selectElement($(this));
    $("#SICategories").hide();
    $("#FactorsCategories").hide();
    buildTree();
    //$("#MetricsCategories").show();
    document.getElementById('MetricsForm').innerHTML = "";
});

/*function list() {
    $.ajax({
        url: '../api/metrics/list',
        type: "GET",
        success: function(categories) {
            console.log(categories);
        }
    });
}*/

function newCategory() {

    $("#SICategories").hide();
    $("#FactorsCategories").hide();
    $("#MetricsCategories").hide();
    document.getElementById('MetricList').innerHTML = "";

    var patternForm = document.createElement('div');
    patternForm.setAttribute("id", "patternForm");

    var title1Row = document.createElement('div');
    title1Row.classList.add("productInfoRow");
    var title1P = document.createElement('p');
    title1P.appendChild(document.createTextNode("Step 1 - Give a name for the category"));
    title1P.setAttribute('style', 'font-size: 36px; margin-right: 1%');
    title1Row.appendChild(title1P);
    patternForm.appendChild(title1Row);

    var nameRow = document.createElement('div');
    nameRow.classList.add("productInfoRow");
    var nameP = document.createElement('p');
    nameP.appendChild(document.createTextNode("Name*: "));
    nameP.setAttribute('style', 'font-size: 18px; margin-right: 1%');
    nameRow.appendChild(nameP);
    var inputName = document.createElement("input");
    inputName.setAttribute('id', 'CategoryName');
    inputName.setAttribute('type', 'text');
    inputName.setAttribute('style', 'width: 100%;');
    inputName.setAttribute('placeholder', 'Write the pattern name here');
    nameRow.appendChild(inputName);
    patternForm.appendChild(nameRow);

    var parameterTitleRow = document.createElement('div');
    parameterTitleRow.classList.add("productInfoRow");
    var parameterTitleP = document.createElement('p');
    parameterTitleP.appendChild(document.createTextNode("Step 2 - Fill the Category information"));
    parameterTitleP.setAttribute('style', 'font-size: 36px; margin-right: 1%');
    parameterTitleRow.appendChild(parameterTitleP);
    patternForm.appendChild(parameterTitleRow);

    var metricTable = document.createElement('table');
    var tableRow = document.createElement('div');
    tableRow.classList.add("productInfoRow");
    metricTable.setAttribute('id', "tableMetrics");
    metricTable.setAttribute('class', "table");
    var metrictr = document.createElement('tr');
    var metrictbody = document.createElement('tbody');
    var thName=document.createElement('th');
    thName.appendChild(document.createTextNode("Type"));
    var thColor=document.createElement('th');
    thColor.appendChild(document.createTextNode("Color"));
    var thUpperThreshold=document.createElement('th');
    thUpperThreshold.appendChild(document.createTextNode("Upper Threshold (%)"));
    var thEmpty = document.createElement('th');
    var thSpan=document.createElement('th');
    var Span = document.createElement('span');
    Span.setAttribute("class","table-addMetric glyphicon glyphicon-plus");
    thSpan.appendChild(Span);
    metrictr.appendChild(thName);
    metrictr.appendChild(thColor);
    metrictr.appendChild(thUpperThreshold);
    metrictr.appendChild(thEmpty);
    metrictr.appendChild(thSpan);
    metrictbody.appendChild(metrictr);
    metricTable.appendChild(metrictbody);
    tableRow.appendChild(metricTable);
    patternForm.appendChild(tableRow);

    var warningDiv = document.createElement('p');
    var warningText =document.createTextNode("Two or more categories have the same name");
    warningDiv.hidden=true;
    warningDiv.setAttribute('style', "color:red;margin-left: 7px;");
    warningDiv.append(warningText);
    warningDiv.setAttribute('id', 'warningId');
    patternForm.appendChild(warningDiv);

    var buttonsRow = document.createElement('div');
    buttonsRow.classList.add("productInfoRow");
    buttonsRow.setAttribute('id', 'buttonsRow');
    buttonsRow.setAttribute('style', 'justify-content: space-between;');
    var saveButton = document.createElement('button');
    saveButton.classList.add("btn");
    saveButton.classList.add("btn-primary");
    saveButton.setAttribute('id', 'saveButton');
    saveButton.setAttribute('style', 'font-size: 18px; max-width: 30%;');
    saveButton.appendChild(document.createTextNode("Save Pattern"));
    saveButton.addEventListener("click", function() { saveMetricCategories();});
    saveMethod = "POST";
    buttonsRow.appendChild(saveButton);
    patternForm.appendChild(buttonsRow);

    document.getElementById('MetricsForm').innerHTML = "";
    document.getElementById('MetricsForm').appendChild(patternForm);

    $('.table-addMetric').click(function () {
        var goodCategory = {
            type: "Good",
            color: "#00ff00",
            upperThreshold: 0
        };
        buildCategoryRow(goodCategory, "tableMetrics", true, true, true);
        markTypeConflicts()
    });


    /*buildCategoryRowForm("#00ff00", 100);
    buildCategoryRowForm("#ff8000", 67);
    buildCategoryRowForm("#ff0000", 33);*/
    buildDefaultThresholdTable("tableMetrics");

}

function selectElement (selectedElement) {
    selectedElement.addClass("active");
    $(".category-element").each(function () {
        if (selectedElement.attr("id") !== $(this).attr("id"))
            $(this).removeClass("active");
    });
}

function checkFirst() {
    $('input[name=upperThres][class!="hide"]').each(function (i) {
        if (i != 0) {
            $(this).prop('readonly', false);
        } else {
            $(this).val(100);
            $(this).prop('readonly', true);
        }
    });
}

function loadSICategories () {
    $.ajax({
        url: '../api/strategicIndicators/categories',
        type: "GET",
        success: function(categories) {
            if (categories.length > 0) {
                categories.forEach(function (category) {
                    buildCategoryRow(category, "tableSI", false, true, false);
                });
            } else {
                buildDefaultSITable();
            }
        }
    });
}

function loadFactorCategories () {
    $.ajax({
        url: '../api/qualityFactors/categories',
        type: "GET",
        success: function(categories) {
            if (categories.length > 0) {
                categories.forEach(function (category) {
                    buildCategoryRow(category, "tableQF", true, true, false);
                });
            } else {
                buildDefaultThresholdTable("tableQF");
            }
        }
    });
}
function buildtable(name) {
    $("#SICategories").hide();
    $("#FactorsCategories").hide();
    $("#MetricsCategories").hide();

    var patternForm = document.createElement('div');
    patternForm.setAttribute("id", "patternForm");

    var title1Row = document.createElement('div');
    title1Row.classList.add("productInfoRow");
    var title1P = document.createElement('p');
    title1P.setAttribute('id', 'Category Name');
    title1P.setAttribute('style', 'font-size: 36px; margin-right: 1%');
    title1Row.appendChild(title1P);
    patternForm.appendChild(title1Row);


    var metricTable = document.createElement('table');
    var tableRow = document.createElement('div');
    tableRow.classList.add("productInfoRow");
    metricTable.setAttribute('id', "tableMetrics");
    metricTable.setAttribute('class', "table");
    var metrictr = document.createElement('tr');
    var metrictbody = document.createElement('tbody');
    var thName=document.createElement('th');
    thName.appendChild(document.createTextNode("Type"));
    var thColor=document.createElement('th');
    thColor.appendChild(document.createTextNode("Color"));
    var thUpperThreshold=document.createElement('th');
    thUpperThreshold.appendChild(document.createTextNode("Upper Threshold (%)"));
    var thEmpty = document.createElement('th');
    var thSpan=document.createElement('th');
    var Span = document.createElement('span');
    Span.setAttribute("class","table-addMetric glyphicon glyphicon-plus");
    thSpan.appendChild(Span);
    metrictr.appendChild(thName);
    metrictr.appendChild(thColor);
    metrictr.appendChild(thUpperThreshold);
    metrictr.appendChild(thEmpty);
    metrictr.appendChild(thSpan);
    metrictbody.appendChild(metrictr);
    metricTable.appendChild(metrictbody);
    tableRow.appendChild(metricTable);
    patternForm.appendChild(tableRow);

    var warningDiv = document.createElement('p');
    var warningText =document.createTextNode("Two or more categories have the same name");
    warningDiv.hidden=true;
    warningDiv.setAttribute('style', "color:red;margin-right: 60px;");
    warningDiv.append(warningText)
    warningDiv.setAttribute('id', 'warningId');
    warningDiv.setAttribute("align", "right")
    patternForm.appendChild(warningDiv);

    var buttonsRow = document.createElement('div');
    buttonsRow.classList.add("productInfoRow");
    buttonsRow.setAttribute('id', 'buttonsRow');
    buttonsRow.setAttribute('style', 'justify-content: space-between;');
    var deleteButton = document.createElement('button');
    deleteButton.classList.add("btn");
    deleteButton.classList.add("btn-primary");
    deleteButton.classList.add("btn-danger");
    deleteButton.setAttribute('id', 'deleteButton');
    deleteButton.setAttribute('style', 'font-size: 18px; max-width: 35%;');
    deleteButton.appendChild(document.createTextNode("Delete Metric Category"));
    deleteButton.addEventListener("click", function() { deleteMetricCategories(name);});
    buttonsRow.appendChild(deleteButton);
    var saveButton = document.createElement('button');
    saveButton.classList.add("btn");
    saveButton.classList.add("btn-primary");
    saveButton.setAttribute('id', 'saveButton');
    saveButton.setAttribute('style', 'font-size: 18px; max-width: 30%;');
    saveButton.appendChild(document.createTextNode("Save Metric Category"));
    saveButton.addEventListener("click", function()  {updateMetricCategories(name);});
    buttonsRow.appendChild(saveButton);
    patternForm.appendChild(buttonsRow);

    document.getElementById('MetricsForm').innerHTML = "";
    document.getElementById('MetricsForm').appendChild(patternForm);

    $('.table-addMetric').click(function () {
        var goodCategory = {
            type: "Good",
            color: "#00ff00",
            upperThreshold: 0
        };
        buildCategoryRow(goodCategory, "tableMetrics", true, true, true);
        markTypeConflicts();
    });

    /*buildCategoryRowForm("#00ff00", 100);
    buildCategoryRowForm("#ff8000", 67);
    buildCategoryRowForm("#ff0000", 33);*/
    //buildDefaultThresholdTable("tableMetrics");


}

function updateMetricCategories (name) {

    var dataMetrics = getDataMetricThreshold("tableMetrics");
    if (dataMetrics.length < 2)
        warningUtils("Warning", "There has to be at least 2 categories for each factor");
    else {
        $.ajax({
            url: '../api/metrics/categories?name=' + name,
            data: JSON.stringify(dataMetrics),
            type: "PUT",
            contentType: "application/json",
            error: function (jqXHR, textStatus, errorThrown) {
                if (jqXHR.status == 409) warningUtils("Error", "You can't have two categories with the same name");
                else warningUtils("Error", "Error on saving categories");
            },
            success: function () {
                warningUtils("Ok", "Metrics Categories saved successfully");

            }
        });
    }
}


function deleteMetricCategories(name) {
    $.ajax({
        url: '../api/metrics/categories?name=' + name,
        type: "DELETE",
        success: function() {
            warningUtils("Ok", "The metric category has been deleted successfully");
            selectElement($(this));
            $("#SICategories").hide();
            $("#FactorsCategories").hide();
            $("#MetricsCategories").hide();
            document.getElementById('MetricsForm').innerHTML = "";
            document.getElementById('MetricList').innerHTML = "";
            buildTree();
            previousSelectionId=null;
        },
        error: function() {
        warningUtils("Error", "Error on deleting category");
        }
    });
}

function loadMetricsCategories (name) {

    document.getElementById("classifier"+name).setAttribute('style', 'background-color: #efeff8;');
    if(previousSelectionId!=null) {
        document.getElementById(previousSelectionId).setAttribute('style', 'background-color: #ffffff;');
    }
    previousSelectionId = "classifier"+name;
    $.ajax({
        url: '../api/metrics/categories?name=' + name,
        type: "GET",
        success: function(categories) {

            if (categories.length > 0) {
                buildtable(name);
                document.getElementById('Category Name').appendChild(document.createTextNode("Metric category name: " +  name));
                categories.forEach(function (category) {
                    buildCategoryRow(category, "tableMetrics", true, true, true);
                });
            } else {
                buildDefaultThresholdTable("tableMetrics");
            }
        }
    });
}

function markTypeConflicts() {

    var data=getDataMetricThreshold("tableMetrics");
    var uniques = [];
    var repetits = false;
    for(let i =0; i<data.length && !repetits; i++) {
        if(uniques.includes(data[i].type)) {
            document.getElementById("saveButton").disabled=true;
            document.getElementById("warningId").hidden=false;
            //warningUtils("Warning", "Two or more category types have the same name");
            repetits=true;
        }
        else uniques.push(data[i].type)
    }
    if(!repetits) {
        document.getElementById("saveButton").disabled=false;
        document.getElementById("warningId").hidden=true;
    }

}

function buildCategoryRow (category, tableId, hasThreshold, canBeEdited, isMetric) {
    var table = document.getElementById(tableId);
    var row = table.insertRow(-1);



    if(canBeEdited) {

        var categoryName = document.createElement("td");
        categoryName.setAttribute("contenteditable", "true");
        if(isMetric) {
            categoryName.appendChild(document.createTextNode(category.type));
            categoryName.addEventListener("input", (event) => markTypeConflicts());
        }

        else categoryName.appendChild(document.createTextNode(category.name));
        row.appendChild(categoryName);

        var categoryColorPicker = document.createElement("input");
        categoryColorPicker.setAttribute("value", category.color);
        categoryColorPicker.setAttribute("type", "color");
        var categoryColor = document.createElement("td");
        categoryColor.appendChild(categoryColorPicker);
        row.appendChild(categoryColor);

        if (hasThreshold) {
            var thresholdSelector = document.createElement("input");
            thresholdSelector.setAttribute("value", category.upperThreshold * 100);
            thresholdSelector.setAttribute("name", "upperThres");
            thresholdSelector.setAttribute("min", "1");
            thresholdSelector.setAttribute("max", "100");
            thresholdSelector.setAttribute("type", "number");
            var threshold = document.createElement("td");
            threshold.appendChild(thresholdSelector);
            row.appendChild(threshold);
        }
    }
    else {
        var categoryName = document.createElement("td");
        categoryName.setAttribute("contenteditable", "false");
        if(isMetric)categoryName.appendChild(document.createTextNode(category.type));
        else categoryName.appendChild(document.createTextNode(category.name));
        row.appendChild(categoryName);

        var categoryColorPicker = document.createElement("input")
        categoryColorPicker.setAttribute("disabled", "true");
        categoryColorPicker.setAttribute("value", category.color);
        categoryColorPicker.setAttribute("type", "color");
        var categoryColor = document.createElement("td");
        categoryColor.appendChild(categoryColorPicker);
        row.appendChild(categoryColor);

        if (hasThreshold) {
            var thresholdSelector = document.createElement("input");
            thresholdSelector.setAttribute("value", category.upperThreshold * 100);
            thresholdSelector.setAttribute("name", "upperThres");
            thresholdSelector.setAttribute("disabled", "true");
            thresholdSelector.setAttribute("min", "1");
            thresholdSelector.setAttribute("max", "100");
            thresholdSelector.setAttribute("type", "number");
            var threshold = document.createElement("td");
            threshold.appendChild(thresholdSelector);
            row.appendChild(threshold);
        }
    }

    if(canBeEdited) {
        var arrowUp = document.createElement("span");
        arrowUp.classList.add("glyphicon", "glyphicon-arrow-up");
        arrowUp.addEventListener("click", function () {
            var $row = $(this).parents('tr');
            if ($row.index() === 1) return; // Don't go above the header
            $row.prev().before($row.get(0));
            checkFirst();
        });
        var arrowDown = document.createElement("span");
        arrowDown.classList.add("glyphicon", "glyphicon-arrow-down");
        arrowDown.addEventListener("click", function () {
            var $row = $(this).parents('tr');
            $row.next().after($row.get(0));
            checkFirst();
        });
        var arrows = document.createElement("td");
        arrows.appendChild(arrowUp);
        arrows.appendChild(arrowDown);
        row.appendChild(arrows);

        var removeIcon = document.createElement("span");
        removeIcon.classList.add("glyphicon", "glyphicon-remove");
        var remove = document.createElement("td");
        remove.addEventListener("click", function () {
            $(this).parents('tr').detach();
            checkFirst();
        });
        remove.appendChild(removeIcon);

        row.appendChild(remove);
    }
}

function buildDefaultSITable () {
    var goodCategory = {
        name: "Good",
        color: "#00ff00"
    };
    buildCategoryRow(goodCategory, "tableSI", false, true, false);

    var neutralCategory = {
        name: "Neutral",
        color: "#ff8000"
    };
    buildCategoryRow(neutralCategory, "tableSI", false, true, false);

    var badCategory = {
        name: "Bad",
        color: "#ff0000"
    };
    buildCategoryRow(badCategory, "tableSI", false, true, false);
}

function buildDefaultThresholdTable (table) {
    var goodCategory = {
        type: "Good",
        name: "Good",
        color: "#00ff00",
        upperThreshold: 1
    };
    buildCategoryRow(goodCategory, table, true, true, true);

    var neutralCategory = {
        type: "Neutral",
        name: "Neutral",
        color: "#ff8000",
        upperThreshold: 0.67
    };
    buildCategoryRow(neutralCategory, table, true, true, true);

    var badCategory = {
        type: "Bad",
        name: "Bad",
        color: "#ff0000",
        upperThreshold: 0.33
    };
    buildCategoryRow(badCategory, table, true, true, true);
}

function addButtonBehaviour () {
    $('.table-addSI').click(function () {
        var goodCategory = {
            name: "Good",
            color: "#00ff00"
        };
        buildCategoryRow(goodCategory, "tableSI", false, true, false);
    });

    $('.table-addQF').click(function () {
        var goodCategory = {
            name: "Good",
            color: "#00ff00",
            upperThreshold: 0
        };
        buildCategoryRow(goodCategory, "tableQF", true, true, false);
    });

    $('.table-addMetric').click(function () {
        var goodCategory = {
            type: "Good",
            color: "#00ff00",
            upperThreshold: 0
        };
        buildCategoryRow(goodCategory, "tableMetrics", true, true, true);
        markTypeConflicts()
    });
}

function getData() {
    var $rows = $('#tableSI').find('tr:not(:hidden)');
    var headers = ["name", "color"];
    var data = [];

    // Turn all existing rows into a loopable array
    $rows.slice(1).each(function () {
        var $td = $(this).find('td');
        var h = {};

        // Use the headers from earlier to name our hash keys
        headers.forEach(function (header, i) {
            if (i%2 == 0)
                h[header] = $td.eq(i).text();
            else
                h[header] = $td.eq(i).children()[0].value;
        });

        data.push(h);
    });
    return data;
}

function getDataThreshold (table) {
    var $rows = $('#'+table).find('tr:not(:hidden)');
    var headers = ["name", "color", "upperThreshold"];
    var data = [];

    // Turn all existing rows into a loopable array
    $rows.slice(1).each(function () {
        var $td = $(this).find('td');
        var h = {};

        // Use the headers from earlier to name our hash keys
        headers.forEach(function (header, i) {
            if (i%3 == 0)
                h[header] = $td.eq(i).text();
            else if (i%3 == 1)
                h[header] = $td.eq(i).children()[0].value;
            else
                h[header] = $td.eq(i).children()[0].value;
        });

        data.push(h);
    });
    console.log(data);
    return data;
}

function getDataMetricThreshold (table) {
    var $rows = $('#'+table).find('tr:not(:hidden)');
    var headers = ["type", "color", "upperThreshold"];
    var data = [];

    // Turn all existing rows into a loopable array
    $rows.slice(1).each(function () {
        var $td = $(this).find('td');
        var h = {};

        // Use the headers from earlier to name our hash keys
        headers.forEach(function (header, i) {
            if (i%3 == 0)
                h[header] = $td.eq(i).text();
            else if (i%3 == 1)
                h[header] = $td.eq(i).children()[0].value;
            else
                h[header] = $td.eq(i).children()[0].value;
        });

        data.push(h);
    });
    //console.log(data);
    return data;
}

$('#saveSICategories').click(function () {
    var dataSI = getData();
    if (dataSI.length < 2)
        warningUtils("Warning", "There has to be at least 2 categories for each indicator");
    else {
        $.ajax({
            url: '../api/strategicIndicators/categories',
            data: JSON.stringify(dataSI),
            type: "POST",
            contentType: "application/json",
            error: function(jqXHR, textStatus, errorThrown) {
                if (jqXHR.status == 405)
                    warningUtils("Error", "You can't have two categories with the same name");
                else
                    warningUtils("Error", "Error on saving categories");
            },
            success: function() {
                warningUtils("Ok","Strategic Indicator Categories saved successfully");
            }
        });

    }
});

$('#saveFactorCategories').click(function () {
    var dataQF = getDataThreshold("tableQF");
    if (dataQF.length < 2)
        warningUtils("Warning", "There has to be at least 2 categories for each factor");
    else {
        $.ajax({
            url: '../api/qualityFactors/categories',
            data: JSON.stringify(dataQF),
            type: "POST",
            contentType: "application/json",
            error: function(jqXHR, textStatus, errorThrown) {
                if (jqXHR.status == 405)
                    warningUtils("Error", "You can't have two categories with the same name");
                else
                    warningUtils("Error","Error on saving categories");
            },
            success: function() {
                warningUtils("Ok", "Quality Factor Categories saved successfully");
            }
        });

    }
});



function saveMetricCategories () {

    var dataMetrics = getDataMetricThreshold("tableMetrics");
    var name = document.getElementById("CategoryName").value;
    if(name=="") {
        warningUtils("Warning", "Make sure that you have completed all fields marked with an *");
        console.log(dataMetrics);
    }
    else {
        if (dataMetrics.length < 2)
            warningUtils("Warning", "There has to be at least 2 categories for each factor");
        else {
            $.ajax({
                url: '../api/metrics/categories?name=' + name,
                data: JSON.stringify(dataMetrics),
                type: "POST",
                contentType: "application/json",
                error: function (jqXHR, textStatus, errorThrown) {
                    if (jqXHR.status == 409)
                        warningUtils("Error", "You can't have two categories with the same name");
                    else
                        warningUtils("Error", "Error on saving categories");
                },
                success: function () {
                    warningUtils("Ok", "Metrics Categories saved successfully");
                    document.getElementById('MetricsForm').innerHTML = "";
                }
            });

        }
    }
}





size = $('input[name=upperThres][class!="hide"]').length;
$('input[name=upperThres][class!="hide"]').each(function (i) {
    $(this).val(Math.round((size-i)*100/size));
});
checkFirst();
addButtonBehaviour();
if (sessionStorage.getItem("profile_qualitylvl") == "METRICS") {
    // hide SI Categories info
    $("#SICategories").hide();
    $("#SICategoriesButton").hide();
    // hide Factor Categories info
    $("#FactorsCategories").hide();
    $("#FactorsCategoriesButton").hide();
    // show Metric Categories info
    //loadMetricsCategories();
    selectElement($("#MetricsCategoriesButton"));
    $("#MetricsCategories").show();
} else if (sessionStorage.getItem("profile_qualitylvl") == "METRICS_FACTORS") {
    // hide SI Categories info
    $("#SICategories").hide();
    $("#SICategoriesButton").hide();
    // show Factor Categories info
    loadFactorCategories();
    selectElement($("#FactorsCategoriesButton"));
    $("#FactorsCategories").show();
    // load Metric Categories info
    //loadMetricsCategories();
} else {
    // load all Categories info
    loadSICategories();
    loadFactorCategories();
    //loadMetricsCategories();
}

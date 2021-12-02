$("#SICategoriesButton").click(function () {
    selectElement($(this));
    $("#SICategories").show();
    $("#FactorsCategories").hide();
    $("#MetricsCategories").hide();
    document.getElementById('MetricsForm').innerHTML = "";
});

$("#FactorsCategoriesButton").click(function () {
    selectElement($(this));
    $("#SICategories").hide();
    $("#FactorsCategories").show();
    $("#MetricsCategories").hide();
    document.getElementById('MetricsForm').innerHTML = "";
});

$("#MetricsCategoriesButton").click(function () {
    selectElement($(this));
    $("#SICategories").hide();
    $("#FactorsCategories").hide();
    $("#MetricsCategories").show();
    document.getElementById('MetricsForm').innerHTML = "";
});

function newCategory() {

    $("#SICategories").hide();
    $("#FactorsCategories").hide();
    $("#MetricsCategories").hide();

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
    inputName.setAttribute('id', 'Category Name');
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
    thName.appendChild(document.createTextNode("Name"));
    var thColor=document.createElement('th');
    thColor.appendChild(document.createTextNode("Color"));
    var thUpperThreshold=document.createElement('th');
    thUpperThreshold.appendChild(document.createTextNode("Upper Threshold (%)"));
    metrictr.appendChild(thName);
    metrictr.appendChild(thColor);
    metrictr.appendChild(thUpperThreshold);
    metrictbody.appendChild(metrictr);
    metricTable.appendChild(metrictbody);
    tableRow.appendChild(metricTable);
    patternForm.appendChild(tableRow);

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
    //saveButton.addEventListener("click", savePattern);
    saveMethod = "POST";
    buttonsRow.appendChild(saveButton);
    patternForm.appendChild(buttonsRow);

    document.getElementById('MetricsForm').innerHTML = "";
    document.getElementById('MetricsForm').appendChild(patternForm);

    buildCategoryRowForm("#00ff00", 100);
    buildCategoryRowForm("#ff8000", 67);
    buildCategoryRowForm("#ff0000", 33);




   /* var metricTable = document.createElement('table');
    var tableRow = document.createElement('div');
    tableRow.classList.add("productInfoRow");
    metricTable.setAttribute('id', "tableMetrics");
    metricTable.setAttribute('class', "table");
    var metrictr = document.createElement('tr');
    var thName=document.createElement('th');
    thName.appendChild(document.createTextNode("Name"));
    var thColor=document.createElement('th');
    thColor.appendChild(document.createTextNode("Color"));
    var thUpperThreshold=document.createElement('th');
    thUpperThreshold.appendChild(document.createTextNode("Upper Threshold (%)"));
    var thEmpty=document.createElement('th');
    var thspan=document.createElement('th');
    var span=document.createElement('span');
    span.setAttribute('class', "table-addMetric glyphicon glyphicon-plus");
    thspan.appendChild(span);
    metrictr.appendChild(thName);
    metrictr.appendChild(thColor);
    metrictr.appendChild(thUpperThreshold);
    metrictr.appendChild(thspan);
    metricTable.appendChild(metrictr);
    tableRow.appendChild(metricTable);
    patternForm.appendChild(tableRow)*/





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
                    buildCategoryRow(category, "tableSI", false);
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
                    buildCategoryRow(category, "tableQF", true);
                });
            } else {
                buildDefaultThresholdTable("tableQF");
            }
        }
    });
}

function loadMetricsCategories () {
    $.ajax({
        url: '../api/metrics/categories',
        type: "GET",
        success: function(categories) {
            if (categories.length > 0) {
                categories.forEach(function (category) {
                    buildCategoryRow(category, "tableMetrics", true);
                });
            } else {
                buildDefaultThresholdTable("tableMetrics");
            }
        }
    });
}

function buildCategoryRowForm (color, Threshold) {
    var table = document.getElementById("tableMetrics");
    var row = table.insertRow(-1);

    var categoryName = document.createElement("td");
    categoryName.setAttribute("contenteditable", "true");
    categoryName.appendChild(document.createTextNode("Write type here"));
    row.appendChild(categoryName);

    var categoryColorPicker = document.createElement("input");
    categoryColorPicker.setAttribute("value", color);
    categoryColorPicker.setAttribute("type", "color");
    var categoryColor = document.createElement("td");
    categoryColor.appendChild(categoryColorPicker);
    row.appendChild(categoryColor);

    var thresholdSelector = document.createElement("input");
    thresholdSelector.setAttribute("value", Threshold);
    thresholdSelector.setAttribute("name", "upperThres");
    thresholdSelector.setAttribute("min", "1");
    thresholdSelector.setAttribute("max", "100");
    thresholdSelector.setAttribute("type", "number");
    var threshold = document.createElement("td");
    threshold.appendChild(thresholdSelector);
    row.appendChild(threshold);


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


function buildCategoryRow (category, tableId, hasThreshold) {
    var table = document.getElementById(tableId);
    var row = table.insertRow(-1);

    var categoryName = document.createElement("td");
    categoryName.setAttribute("contenteditable", "true");
    categoryName.appendChild(document.createTextNode(category.name));
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

function buildDefaultSITable () {
    var goodCategory = {
        name: "Good",
        color: "#00ff00"
    };
    buildCategoryRow(goodCategory, "tableSI", false);

    var neutralCategory = {
        name: "Neutral",
        color: "#ff8000"
    };
    buildCategoryRow(neutralCategory, "tableSI", false);

    var badCategory = {
        name: "Bad",
        color: "#ff0000"
    };
    buildCategoryRow(badCategory, "tableSI", false);
}

function buildDefaultThresholdTable (table) {
    var goodCategory = {
        name: "Good",
        color: "#00ff00",
        upperThreshold: 1
    };
    buildCategoryRow(goodCategory, table, true);

    var neutralCategory = {
        name: "Neutral",
        color: "#ff8000",
        upperThreshold: 0.67
    };
    buildCategoryRow(neutralCategory, table, true);

    var badCategory = {
        name: "Bad",
        color: "#ff0000",
        upperThreshold: 0.33
    };
    buildCategoryRow(badCategory, table, true);
}

function addButtonBehaviour () {
    $('.table-addSI').click(function () {
        var goodCategory = {
            name: "Good",
            color: "#00ff00"
        };
        buildCategoryRow(goodCategory, "tableSI", false);
    });

    $('.table-addQF').click(function () {
        var goodCategory = {
            name: "Good",
            color: "#00ff00",
            upperThreshold: 0
        };
        buildCategoryRow(goodCategory, "tableQF", true);
    });

    $('.table-addMetric').click(function () {
        var goodCategory = {
            name: "Good",
            color: "#00ff00",
            upperThreshold: 0
        };
        buildCategoryRow(goodCategory, "tableMetrics", true);
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

$('#saveMetricCategories').click(function () {
    var dataMetrics = getDataThreshold("tableMetrics");
    if (dataMetrics.length < 2)
        warningUtils("Warning", "There has to be at least 2 categories for each factor");
    else {
        $.ajax({
            url: '../api/metrics/categories',
            data: JSON.stringify(dataMetrics),
            type: "POST",
            contentType: "application/json",
            error: function(jqXHR, textStatus, errorThrown) {
                if (jqXHR.status == 405)
                    warningUtils("Error", "You can't have two categories with the same name");
                else
                    warningUtils("Error","Error on saving categories");
            },
            success: function() {
                warningUtils("Ok","Metrics Categories saved successfully");
            }
        });

    }
});


size = $('input[name=upperThres][class!="hide"]').length;
$('input[name=upperThres][class!="hide"]').each(function (i) {
    $(this).val(Math.round((size-i)*100/size));
});
checkFirst();
if (sessionStorage.getItem("profile_qualitylvl") == "METRICS") {
    // hide SI Categories info
    $("#SICategories").hide();
    $("#SICategoriesButton").hide();
    // hide Factor Categories info
    $("#FactorsCategories").hide();
    $("#FactorsCategoriesButton").hide();
    // show Metric Categories info
    loadMetricsCategories();
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
    loadMetricsCategories();
} else {
    // load all Categories info
    loadSICategories();
    loadFactorCategories();
    loadMetricsCategories();
}
addButtonBehaviour();
console.log("sessionStorage: profile_id");
console.log(sessionStorage.getItem("profile_id"));
var profileId = sessionStorage.getItem("profile_id");
var url = "/api/strategicIndicators/qualityModel?profile=" +profileId;
var serverUrl = sessionStorage.getItem("serverUrl");
if (serverUrl) {
    url = serverUrl + url;
}

var prj = "test";
//var prj = sessionStorage.getItem("prj");

var metricColor = "#61B8E5";
var factorColor = "orange";
var siColor = "lightgray";

function loadData() {
    jQuery.ajax({
        dataType: "json",
        type: "GET",
        url : url,
        async: true,
        success: function (data) {
            buildTree(data);
        }});
}

function buildTree(strategicIndicators) {
    var qmnodes = new Map();
    var qmedges = new Map();
    console.log(strategicIndicators);
    for (var i = 0; i < strategicIndicators.length; i++) {
        var strategicIndicator = strategicIndicators[i];
        var node = createNode(strategicIndicator, siColor, strategicIndicator.color);
        if (!qmnodes.has(strategicIndicator.id) && sessionStorage.getItem("profile_qualitylvl") == "ALL")
            qmnodes.set(strategicIndicator.id, node);
        for (var j = 0; j < strategicIndicator.factors.length; j++) {
            var factor = strategicIndicator.factors[j];
            var node = createNode(factor, factorColor, factorColor);
            if (!qmnodes.has(factor.id))
                qmnodes.set(factor.id, node);
            if (!qmedges.has(factor.id+"-"+strategicIndicator.id) && sessionStorage.getItem("profile_qualitylvl") == "ALL")
                qmedges.set(factor.id+"-"+strategicIndicator.id, createEdge(factor, strategicIndicator));
            for (var k = 0; k < factor.metrics.length; k++) {
                var metric = factor.metrics[k];
                var node = createNode(metric, metricColor, metricColor);
                if (!qmnodes.has(metric.id))
                    qmnodes.set(metric.id, node);
                if (!qmedges.has(metric.id+"-"+factor.id))
                    qmedges.set(metric.id+"-"+factor.id, createEdge(metric, factor));
            }
        }
    }
    displayData(Array.from(qmnodes.values()), Array.from(qmedges.values()));
}

function createNode (element, color, colorBorder) {
    var value;
    if (element.valueDescription) value = element.valueDescription;
    else {
        value = element.assessmentValue;
    }
    return {
        data: {
            id: element.id,
            label: element.name + ": " + parseValue(value),
            color: color,
            colorBorder: colorBorder
        }
    }
}


function createEdge (source, target) {
    var weight = source.weight;
    if (weight == -1) weight = null;
    else weight = (parseFloat(weight) * 100).toFixed(0) + "%"; // weight percentage
    return {
        data: {
            source: source.id,
            target: target.id,
            weight: weight
        }
    }
}

function parseValue(value) {
    if (isNumeric(value)) {
        value = parseFloat(value).toFixed(2);
    }
    return value;
}

function isNumeric(value) {
    return !isNaN(value);
}

function displayData(qmnodes, qmedges) {
    var cy = window.cy = cytoscape({
        container: document.getElementById('cy'),
        boxSelectionEnabled: false,
        autounselectify: true,
        layout: {
            name: 'dagre',
            rankDir : 'RL',
            ranker: 'longest-path',
            rankSep: 300
        },
        style: [
            {
                selector: 'node',
                style: {
                    'color' : 'black',
                    'width': 'label',
                    'text-halign' : 'center',
                    'text-valign' : 'center',
                    'shape' : 'roundrectangle',
                    'content': 'data(label)',
                    'background-color': 'data(color)',
                    'padding' : '5px',
                    'border-style' : 'solid',
                    'border-color' : 'data(colorBorder)',
                    'border-width' : 5
                }
            },
            {
                selector: 'edge',
                style: {
                    'width': 4,
                    'target-arrow-shape': 'triangle',
                    'line-color': 'lightgray',
                    'target-arrow-color': 'lightgray',
                    'curve-style': 'bezier',
                    'label' : 'data(weight)',
                    'font-size' : '12px'
                }
            }
        ],
        elements: {
            nodes: qmnodes,
            edges: qmedges
        }
    });

    cy.on('click', 'node', function(){
        showNodeDetails(this._private.data);
    });
}

function showNodeDetails(node) {
    var serverUrl = sessionStorage.getItem("serverUrl");
    var profileId = sessionStorage.getItem("profile_id");
    if (node.color === siColor) {
        var urlSI = "/api/strategicIndicators/"+ node.id + "/current?profile=" + profileId;
        if (serverUrl) {
            urlSI = serverUrl + urlSI;
        }
        getAndShowElement(urlSI);
    }
    else if (node.color === factorColor) {
        var urlFactor = "/api/qualityFactors/"+ node.id + "/current";
        if (serverUrl) {
            urlFactor = serverUrl + urlFactor;
        }
        getAndShowElement(urlFactor);
    }
    else {
        var urlMetric = "/api/metrics/"+ node.id + "/current";
        if (serverUrl) {
            urlMetric = serverUrl + urlMetric;
        }
        getAndShowElement(urlMetric);
    }
}

function getAndShowElement (url) {
    jQuery.ajax({
        dataType: "json",
        type: "GET",
        url : url,
        async: true,
        success: function (element) {
            $("#qualityModelElementModalTitle").text(element.name);
            $("#qualityModelElementValue").val(element.value_description);
            $("#qualityModelElementDescription").val(element.description);
            if (element.hasOwnProperty("rationale")) {
                $("#qualityModelElementRationaleDiv").show();
                $("#qualityModelElementRationale").val(element.rationale);
            }
            else {
                $("#qualityModelElementRationaleDiv").hide();
            }
            $("#qualityModelElementModal").modal();
        }});
}

window.onload = function() {
    var QMView = document.getElementById('QMView');
    QMView.hidden = true;
    if (sessionStorage.getItem("profile_qualitylvl") == "ALL") {
        QMView.hidden = false;
    }
    loadData();
}

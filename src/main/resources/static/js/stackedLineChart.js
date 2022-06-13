var timeFormat = 'YYYY-MM-DD';
var config = [];
var charts = [];
var urlTaiga;
var urlGithub;
var colors = ['rgb(1, 119, 166)', 'rgb(255, 153, 51)', 'rgb(51, 204, 51)', 'rgb(255, 80, 80)', 'rgb(204, 201, 53)', 'rgb(192, 96, 201)'];
var decisionIgnoreColor = 'rgb(189,0,0)';
var decisionAddColor = 'rgb(62,208,62)';

Chart.plugins.register({
    afterDraw: function(chart) {
        var allEmpty = true;
        for (var i = 0; i < chart.data.datasets.length; i++) {
            if (chart.data.datasets[i].data.length > 0) allEmpty = false;
        }
        if (allEmpty) {
            // No data is present
            var ctx = chart.chart.ctx;
            var width = chart.chart.width;
            var height = chart.chart.height;
            chart.clear();

            ctx.save();
            ctx.textAlign = 'center';
            ctx.textBaseline = 'middle';
            ctx.font = "Helvetica Nueue";
            ctx.fillText(chart.data.errors[0], width / 2, height / 2, width);
            ctx.restore();
        }
    }
});

function getCurrentProject() {

    var urlp = "/api/projects";
    jQuery.ajax({
        dataType: "json",
        url: urlp,
        cache: false,
        type: "GET",
        async: false,
        success: function (data) {
            for(var i=0; i<data.length; i++) {
                if(data[i].name===sessionStorage.getItem("prj")) {
                    urlTaiga = data[i].taigaURL;
                    urlGithub = data[i].githubURL;
                }
            }
        }
    });
}

function drawChart() {
    getCurrentProject()
    config = [];
    for (var i = 0; i < texts.length; ++i) {    //create config for each chart
        var c = {
            type: 'line',
            data: {
                datasets: [],
                errors: []
            },
            options: {
                title: {
                    display: false,
                    fontSize: 16,
                    text: texts[i]
                },
                responsive: false,
                legend: {
                    display: true,
                    labels: {
                        boxWidth: 13,
                        generateLabels: function (chart) {
                            var data = chart.data;
                            var maxLength = Math.round(70/data.datasets.length);

                            return data.datasets.map(function (dataset, i) {
                                var label = dataset.label;
                                if (label.length > maxLength + 3) {
                                    label = label.substring(0, maxLength) + "...";
                                }

                                return {
                                    text: label,
                                    fillStyle: dataset.backgroundColor,
                                    strokeStyle: dataset.borderColor,
                                    lineWidth: dataset.borderWidth,
                                    hidden: dataset.hidden,
                                    index: i
                                }
                            })
                        }
                    },
                    onClick: function(e, legendItem) {
                        var index = legendItem.index;
                        var chart = this.chart;
                        chart.data.datasets[index].hidden = !chart.data.datasets[index].hidden;
                        chart.update();
                    }
                },
                scales: {
                    xAxes: [{
                        type: "time",
                        time: {
                            unit: 'day',
                            parser: timeFormat,
                            tooltipFormat: 'll'
                        },
                        scaleLabel: {
                            display: true,
                            labelString: 'Date'
                        }
                    }],
                    yAxes: [{
                        scaleLabel: {
                            display: true,
                            labelString: 'value'
                        },
                        ticks: {
                            max: 1.0,
                            min: 0
                        }
                    }]
                },
                tooltips: {
                    enabled: false,
                    custom: function(tooltipModel) {
                        // Tooltip Element
                        var tooltipEl = document.getElementById('chartjs-tooltip');

                        // Create element on first render
                        if (!tooltipEl) {
                            tooltipEl = document.createElement('div');
                            tooltipEl.id = 'chartjs-tooltip';
                            tooltipEl.innerHTML = '<table></table>';
                            document.body.appendChild(tooltipEl);
                        }

                        // Hide if no tooltip
                        if (tooltipModel.opacity === 0) {
                            tooltipEl.style.opacity = 0;
                            return;
                        }

                        // Set caret Position
                        tooltipEl.classList.remove('above', 'below', 'no-transform');
                        if (tooltipModel.yAlign) {
                            tooltipEl.classList.add(tooltipModel.yAlign);
                        } else {
                            tooltipEl.classList.add('no-transform');
                        }

                        function getBody(bodyItem) {
                            return bodyItem.lines;
                        }

                        // Set Text
                        if (tooltipModel.body) {
                            var titleLines = tooltipModel.title || [];
                            var bodyLines = tooltipModel.body.map(getBody);

                            var innerHtml = '<thead>';

                            titleLines.forEach(function(title) {
                                innerHtml += '<tr><th>' + title + '</th></tr>';
                            });
                            innerHtml += '</thead><tbody>';

                            bodyLines.forEach(function(body, i) {
                                var colors = tooltipModel.labelColors[i];
                                var style = 'background:' + colors.backgroundColor;
                                style += '; border-color:' + colors.borderColor;
                                style += '; border-width: 2px';
                                var span = '<span class="chartjs-tooltip-key" style="' + style + '"></span>';
                                if (body[0].indexOf("<b>") === 0)
                                    innerHtml += '<tr><td>' + body + '</td></tr>';
                                else
                                    innerHtml += '<tr><td>' + span + body + '</td></tr>';
                            });
                            innerHtml += '</tbody>';

                            var tableRoot = tooltipEl.querySelector('table');
                            tableRoot.innerHTML = innerHtml;
                        }

                        // `this` will be the overall tooltip
                        var position = this._chart.canvas.getBoundingClientRect();

                        // Display, position, and set styles for font
                        tooltipEl.style.opacity = 1;
                        tooltipEl.style.position = 'absolute';
                        tooltipEl.style.left = position.left + window.pageXOffset + tooltipModel.caretX + 'px';
                        tooltipEl.style.top = position.top + window.pageYOffset + tooltipModel.caretY + 'px';
                        tooltipEl.style.fontFamily = tooltipModel._bodyFontFamily;
                        tooltipEl.style.fontSize = tooltipModel.bodyFontSize + 'px';
                        tooltipEl.style.fontStyle = tooltipModel._bodyFontStyle;
                        tooltipEl.style.padding = tooltipModel.yPadding + 'px ' + tooltipModel.xPadding + 'px';
                        tooltipEl.style.pointerEvents = 'none';
                    }
                },
                annotation: {
                    annotations: []
                }
            }
        };

        for (j = 0; j < value[i].length; ++j) {
            if (value[i][j].length === 0) hidden = true;
            var showLine = true;
            var pointStyle = 'circle';
            var pointRadius = 3;
            var borderWidth = 1;
            var color = colors[j % colors.length];
            // special logic to show decisions in historical views
            if (value[i][j][0] && value[i][j][0].y >= 1.1) {
                showLine = false;
                pointRadius = 5;
                borderWidth = 2;
                // on axis y = 1.1 is shown added decisions
                if (value[i][j][0].y === 1.1) {
                    color = decisionAddColor;
                    pointStyle = 'cross';
                }
                // on axis y = 1.2 is shown ignored decisions
                if (value[i][j][0].y === 1.2) {
                    color = decisionIgnoreColor;
                    pointStyle = 'crossRot';
                }
            }
            console.log("data");
            console.log(value[i][j]);
            c.data.datasets.push({
                label: labels[i][j],
                hidden: false,
                backgroundColor: color,
                borderColor: color,
                fill: false,
                data: value[i][j],
                trendlineLinear: {
                    style: color,
                    lineStyle: "dotted",
                    width: 2
                },
                showLine: showLine,
                pointStyle: pointStyle,
                radius: pointRadius,
                borderWidth: borderWidth
            });

            if (!showLine) {
                c.options.tooltips.callbacks = {
                    label: function (tooltipItems, data) {
                        var posY = data.datasets[tooltipItems.datasetIndex].data[0].y;
                        if (posY === 1.1 || posY === 1.2) {
                            return "<b>Requirement: </b>" + data.datasets[tooltipItems.datasetIndex].data[tooltipItems.index].requirement + "<br/>" +
                                "<b>Comments: </b>" + data.datasets[tooltipItems.datasetIndex].data[tooltipItems.index].comments;
                        } else
                            return data.datasets[tooltipItems.datasetIndex].label + ': ' + tooltipItems.yLabel;
                    }
                };
            }

            if (typeof errors !== 'undefined') {
                c.data.errors.push(errors[i][j]);
            }
            else {
                c.data.errors.push("No data to display");
            }
        }

        //Add category lines
        if (typeof categories !== 'undefined') {
            var annotations = [];
            let metricCategory;

            if (typeof orderedMetricsDB !== 'undefined' && orderedMetricsDB.length !== 0) {
                //for Historic Metrics (parseDataMetricsHistorical.js)
                metricCategory = categories.filter(function (cat) {
                    return cat.name === orderedMetricsDB[i].categoryName;
                });
            } else if (typeof orderedFactorsDB !== 'undefined' && orderedFactorsDB.length !== 0) {
                //for Historic Factors (parseDataQFHistorical.js)
                metricCategory = categories.filter(function (cat) {
                    return cat.name === orderedFactorsDB[i].categoryName;
                });
            } else if (typeof metricsDB !== 'undefined' && metricsDB.length !== 0) {
                //for Historic Detailed Factors (parseDataDetailedQFHistorical.js)
                let catName = getFactorCategory(labels[i], metricsDB);
                metricCategory = categories.filter(function (cat) {
                    return cat.name === catName;
                });

            } else {
                metricCategory = categories.filter(function (cat) {
                    return cat.name === DEFAULT_CATEGORY;
                });
            }


            metricCategory.sort( function (cat1, cat2) {
                return cat1.upperThreshold - cat2.upperThreshold;
            });

            let start = 0;
            metricCategory.forEach( function (category) {
                let end = category.upperThreshold;
                let lineHighCategory = {
                    type: 'line',
                    drawTime: 'beforeDatasetsDraw',
                    mode: 'horizontal',
                    scaleID: 'y-axis-0',
                    value: (start + end) / 2,
                    borderColor: category.color,
                    borderWidth: 1,
                    label: {
                        enabled: false,
                        content: category.name
                    }
                };
                start = end;
                annotations.push(lineHighCategory);
            });

            c.options.annotation.annotations = annotations;
        }

        config.push(c);
    }

    //threshold that marks when to change factor
    let factorIndex = 0;
    let factorThreshold = 0;
    let studentIndex = 0;
    let studentThreshold = 0;

    for (i = 0; i < texts.length; ++i) {
        var a = document.createElement('a');
        var currentURL = window.location.href;
        if (isdsi) {  //if it is a Stacked Line Chart for Detailed Strategic Indicators
            urlLink = "../QualityFactors/HistoricChart?id=" + ids[i] + "&name=" + texts[i];
            a.setAttribute("href", urlLink);
        } else if (isqf) { //if it is a Stacked Line Chart for Quality Factors
            var name = getParameterByName('name');
            var id = getParameterByName('id');
            if (name.length != 0) {//if we know from which Detailed Strategic Indicator we are coming
                if (currentURL.match("/PredictionChart")) urlLink = "../DetailedQualityFactors/PredictionChart?id=" + ids[i] + "&si=" + name + "&siid=" + id + "&name=" + texts[i];
                else urlLink = "../DetailedQualityFactors/HistoricChart?id=" + ids[i] + "&si=" + name + "&siid=" + id + "&name=" + texts[i];
            }
            else {
                if (currentURL.match("/PredictionChart")) urlLink = "../DetailedQualityFactors/PredictionChart?id=" + ids[i] + "&name=" + texts[i];
                else urlLink = "../DetailedQualityFactors/HistoricChart?id=" + ids[i] + "&name=" + texts[i];
            }
            a.setAttribute("href", urlLink);
        } else if (isdqf) { //if it is a Stacked Line Chart for Detailed Quality Factors
            var name = getParameterByName('si');
            var id = getParameterByName('siid');
            if (name.length != 0) {//if we know from which Detailed Strategic Indicator we are coming
                if (currentURL.match("/PredictionChart")) urlLink = "../Metrics/PredictionChart?id=" + ids[i] + "&si=" + name + "&siid=" + id + "&name=" + texts[i];
                else urlLink = "../Metrics/HistoricChart?id=" + ids[i] + "&si=" + name + "&siid=" + id + "&name=" + texts[i];
            }
            else {
                urlLink = "../Metrics/HistoricChart?id=" + ids[i] + "&name=" + texts[i];
            }
            a.setAttribute("href", urlLink);
        } else if (isSi) {
            //if its a SI chart make it a hyperlink
            urlLink = "../DetailedStrategicIndicators/HistoricChart?id=" + ids[i] + "&name=" + texts[i];
            a.setAttribute("href", urlLink);
        } else { // case of metrics link
            console.log(ids[i]);
            var findMet = metricsDB.find(function (element) {
                return element.externalId === ids[i];
            });
            if (findMet)  // if metric not found it will be undefined
                urlLink = findMet.webUrl;
            if (urlLink) {
                a.setAttribute("href", urlLink);
                a.setAttribute("target","_blank")
            }
        }
        a.innerHTML = texts[i];
        a.style.fontSize = "16px";
        var div = document.createElement('div');
        div.style.display = "inline-block";
        var p = document.createElement('p');
        var ctx = document.createElement('canvas');
        ctx.id = 'canvas' + i;
        ctx.width = 350;
        ctx.height = 350;
        ctx.style.display = "inline";

        let studentName;

        if(printMetrics && groupByStudent) {
            if (i === studentThreshold) {

                console.log(students)
                if (studentIndex < students.length) {
                    studentName = students[studentIndex][0];
                    studentThreshold += students[studentIndex][1];
                    studentIndex++;
                }
                var divF = document.createElement('div');
                divF.style.marginTop = "3em";
                divF.style.marginBottom = "1em";
                var labelF = document.createElement('label');

                //labelF.id = factorId;
                labelF.textContent = studentName;
                divF.appendChild(labelF);
                document.getElementById("chartContainer").appendChild(divF)
            }

        }
        else if(printMetrics && i === factorThreshold) {
            let factorId;
            let factorName;
            let factorDescription;
            let factorType;
            if(factorIndex < factors.length) {
                factorId = factors[factorIndex].id;
                factorName = factors[factorIndex].name;
                factorDescription=factors[factorIndex].description
                factorThreshold += factors[factorIndex].metrics.length
                factorType=factors[factorIndex].type
                factorIndex++;
            }else{
                factorId = "withoutfactor"
                factorName = "Metrics not associated to any factor"
            }
            var divF = document.createElement('div');
            divF.style.marginTop = "3em";
            divF.style.marginBottom = "1em";

            if (groupByFactor && factorType === "Taiga") {
                if (urlTaiga !== undefined && urlTaiga!==null) {
                    var b = document.createElement('a')
                    b.href=urlTaiga;
                    var icon = document.createElement("img");
                    icon.src = "../icons/taiga_icon.png"
                    icon.width = 38;
                    icon.height = 25;
                    icon.style = "padding-right:15px;";
                    b.appendChild(icon)
                    divF.appendChild(b);
                }
            }
            if (groupByFactor && factorType === "Github") {
                if (urlGithub !== undefined && urlGithub !== null) {
                    var list = urlGithub.split(";");
                    var b = document.createElement('a')
                    b.href=list[0];
                    var icon1 = document.createElement("img");
                    icon1.src = "../icons/github_icon.png"
                    icon1.width = 38;
                    icon1.height = 25;
                    icon1.style = "padding-right:15px;";
                    b.appendChild(icon1)
                    divF.appendChild(b);
                    if (list.length == 2) {
                        var b = document.createElement('a')
                        b.href=list[1];
                        var icon2 = document.createElement("img");
                        icon2.src = "../icons/github_icon.png"
                        icon2.width = 38;
                        icon2.height = 25;
                        icon2.style = "padding-right:15px;";
                        b.appendChild(icon2)
                        divF.appendChild(b);
                    }
                }
            }

            var labelF = document.createElement('label');
            labelF.id = factorId;
            labelF.textContent = factorName;
            divF.appendChild(labelF);

            var b=document.createElement('a')
            b.classList.add("check")
            b.setAttribute('data-tooltip', factorDescription)
            var tooltipdiv = document.createElement('div');
            tooltipdiv.classList.add("tooltip");
            var iconF = document.createElement('img');
            iconF.class="icons";
            iconF.src="../icons/information.png";
            iconF.width = 38;
            iconF.height = 25;
            iconF.style = "padding-left:15px;";

            var spantootlip = document.createElement('span');
            spantootlip.classList.add("tooltiptext");
            tooltipdiv.appendChild(iconF)
            tooltipdiv.appendChild(spantootlip)

            b.appendChild(iconF)
            if (factorIndex < factors.length) {
                spantootlip.innerHTML = factors[factorIndex].description;
            }
            if (factorId != "withoutfactor"){
                console.log(factorId);
                divF.appendChild(b);
            }

            document.getElementById("chartContainer").appendChild(divF)
        }

        document.getElementById("chartContainer").appendChild(div).appendChild(ctx);
        div.appendChild(p).appendChild(a);
        ctx.getContext("2d");

        var chart = new Chart(ctx, config[i]);
        charts.push(chart);
        window.myLine = chart;  //draw chart
    }

    var fit = sessionStorage.getItem("fitToContent");
    if (fit === "true") {
        $("#fitToContent").prop("checked", true);
        fitToContent();
    } else {
        $("#fitToContent").prop("checked", false);
        normalRange();
    }
}

function getMax (datasets) {
    var max = 0;
    for(var i = 0; i < datasets.length; i++) {
        var dataset = datasets[i];
        if(datasets[i].hidden) {
            continue;
        }
        dataset.data.forEach(function(d) {
            if(d.y > max) {
                max = d.y
            }
        });
    }
    return max;
}

function getMin (datasets) {
    var min = 1;
    for(var i = 0; i < datasets.length; i++) {
        var dataset = datasets[i];
        if(datasets[i].hidden) {
            continue;
        }
        dataset.data.forEach(function(d) {
            if(d.y < min) {
                min = d.y
            }
        });
    }
    return min;
}

$("#fitToContent").change(function () {
    if ($(this).is(":checked")) {
        sessionStorage.setItem("fitToContent", "true");
        fitToContent();
    } else {
        sessionStorage.setItem("fitToContent", "false");
        normalRange();
    }
});



function fitToContent() {
    charts.forEach(function (chart) {
        console.log(chart);
        var max = getMax(chart.config.data.datasets);
        var min = getMin(chart.config.data.datasets);
        if (max === min) {
            max += 0.001;
            min -= 0.001;
        }
        chart.config.options.scales.yAxes[0].ticks.max = max;
        chart.config.options.scales.yAxes[0].ticks.min = min;

        chart.config.options.legend.onClick = function (e, legendItem) {
            var index = legendItem.index;
            var c = this.chart;
            c.data.datasets[index].hidden = !c.data.datasets[index].hidden;
            var max = getMax(c.data.datasets);
            var min = getMin(c.data.datasets);
            if (max === min) {
                max += 0.001;
                min -= 0.001;
            }
            c.options.scales.yAxes[0].ticks.max = max;
            c.options.scales.yAxes[0].ticks.min = min;
            c.update();
        };

        chart.update();
    });
}

// if the factors have the same category, this category is returned
// else the default category is returned
function getFactorCategory(factorNames, factorList) {
    let f1 = factorList.find( function (elem) {
        return elem.name === factorNames[0]
    });

    if (factorNames.length === 1) return f1.categoryName;

    for(let i = 1; i < factorNames.length; ++i){
        let f2 = factorList.find( function (elem) {
            return elem.name === factorNames[i]
        });
        if(f1.categoryName !== f2.categoryName) return DEFAULT_CATEGORY;
        f1 = f2;
    }
    return f1.categoryName;
}

function normalRange() {
    charts.forEach(function (chart) {
        chart.config.options.scales.yAxes[0].ticks.max = 1.0;
        chart.config.options.scales.yAxes[0].ticks.min = 0;

        chart.config.options.legend.onClick = function(e, legendItem) {
            var index = legendItem.index;
            var chart = this.chart;
            chart.data.datasets[index].hidden = !chart.data.datasets[index].hidden;
            chart.update();
        };

        chart.update();
    });
}


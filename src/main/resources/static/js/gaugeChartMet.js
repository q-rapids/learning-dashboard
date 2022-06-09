//get data from API
var feed;

var lowThresh;
var upperThresh;
var angle;
var target;
var tau = Math.PI / 2;
var id = false;
var urlTaiga;
var urlGithub;

var factors;
var students;

const DEFAULT_CATEGORY = "Default";

var url;
if (getParameterByName('id').length !== 0) {
    id = true;
    url = parseURLComposed("../api/qualityFactors/metrics/currentcurrent");
} else {
    var profileId = sessionStorage.getItem("profile_id");
    url = parseURLComposed("../api/metrics/current?profile="+profileId);
}

var metricsDB = [];

var urlLink;

var groupByFactor = true;
var groupByStudent = false;
var groupByTeam = false;

function setUpGroupSelector(global){

    if(Boolean(sessionStorage.getItem("groupByFactor")) === false) {
        //inizializing groupBy cookies
        sessionStorage.setItem("groupByFactor", "true");
        sessionStorage.setItem("groupByStudent", "false");
        sessionStorage.setItem("groupByTeam", "false");
    }

    groupByFactor = sessionStorage.getItem("groupByFactor") === "true";
    groupByStudent = sessionStorage.getItem("groupByStudent") === "true";
    groupByTeam = sessionStorage.getItem("groupByTeam") === "true";

    $("#selectorDropdownItems").append('<li><a onclick="clickFactorSelector()" href="#"> Group by factor </a></li>');
    if(global){
        $("#selectorDropdownItems").append('<li><a onclick="clickTeamSelector()" href="#"> Group by team </a></li>');
    }else{
        $("#selectorDropdownItems").append('<li><a onclick="clickStudentSelector()" href="#"> Group by student </a></li>');
    }

    if(groupByTeam) $("#selectorDropdownText").text('Group by team');
    else if(groupByStudent) $("#selectorDropdownText").text('Group by student');
    else $("#selectorDropdownText").text('Group by factor');
}

function clickFactorSelector(){
    sessionStorage.setItem("groupByFactor", "true");
    sessionStorage.setItem("groupByStudent", "false");
    sessionStorage.setItem("groupByTeam", "false");

    $("#selectorDropdownText").text('Group by factor');

    location.href = serverUrl + "/Metrics/CurrentChartGauge";
}

function clickStudentSelector() {
    sessionStorage.setItem("groupByFactor", "false");
    sessionStorage.setItem("groupByStudent", "true");
    sessionStorage.setItem("groupByTeam", "false");

    $("#selectorDropdownText").text('Group by student');

    location.href = serverUrl + "/Metrics/CurrentChartGauge";
}

function clickTeamSelector() {
    sessionStorage.setItem("groupByFactor", "false");
    sessionStorage.setItem("groupByStudent", "false");
    sessionStorage.setItem("groupByTeam", "true");

    $("#selectorDropdownText").text('Group by team');

    location.href = serverUrl + "/Metrics/CurrentChartGauge";
}

function getData(width, height) {
    getCurrentProject()
    jQuery.ajax({
        dataType: "json",
        url: url,
        cache: false,
        type: "GET",
        async: false,
        success: function (data) {
            sortDataAlphabetically(data);
            jQuery.ajax({
                dataType: "json",
                url: "../api/metrics",
                cache: false,
                type: "GET",
                async: false,
                success: function (dataDB) {
                    metricsDB = dataDB;
                    if (groupByStudent.valueOf() == true) getStudents(data,width,height)
                    else getFactors(data, width, height);

                    //getMetricsCategories(data, width, height);
                }});
        },
        error: function(jqXHR, textStatus, errorThrown) {
            if (jqXHR.status == 409)
                warningUtils("Error","Your datasource and DB categories IDs do not match.");
            else if (jqXHR.status == 400) {
                warningUtils("Error", "Datasource connection failed.");
            }
        }
    });
}

function getStudents(data, width, height) {
    if (id)
        url = parseURLComposed("../api/metrics/student");
    else
        url = "../api/metrics/student"
    jQuery.ajax({
        dataType: "json",
        url: url,
        cache: false,
        type: "GET",
        async: false,
        success: function (dataS) {
            students = dataS;
            console.log("students");
            console.log(students);
            getMetricsCategories(data, width, height);
        }
    });
}

function getFactors(data, width, height) {
    if (id)
        url = parseURLComposed("../api/qualityFactors/metrics/current");
    else
        url = "../api/qualityFactors/metrics/current?profile=" + profileId
    jQuery.ajax({
        dataType: "json",
        url: url,
        cache: false,
        type: "GET",
        async: false,
        success: function (dataF) {
            sortMyDataAlphabetically(dataF);
            factors = dataF;
            console.log("factors");
            console.log(factors);
            getMetricsCategories(data, width, height);
        }
    });
}

function sortMyDataAlphabetically (factors) {
    function compare (a, b) {
        if (a.name < b.name) return -1;
        else if (a.name > b.name) return 1;
        else return 0;
    }
    factors.sort(compare);
}

function getMetricsCategories (data, width, height) {
    jQuery.ajax({
        url: "../api/metrics/categories",
        type: "GET",
        async: false,
        success: function (categories) {
            console.log("groupByFactor " + groupByFactor);
            if (id) { // in case we show metrics for one detailed factor
                if (groupByFactor.valueOf() == true)
                    drawChartByFactor(data[0].metrics, "#gaugeChart", width, height, categories);
                else if (groupByStudent.valueOf() == true)
                    drawChartByStudent(data[0].metrics, "#gaugeChart", width, height, categories);
                else drawChart(data[0].metrics, "#gaugeChart", width, height, categories);
            } else { // in case we show all metrics
                if (groupByFactor.valueOf() == true)
                    drawChartByFactor(data, "#gaugeChart", width, height, categories);
                else if (groupByStudent.valueOf() == true)
                    drawChartByStudent(data[0].metrics, "#gaugeChart", width, height, categories);
                else drawChart(data, "#gaugeChart", width, height, categories);
            }
        }
    });
}

function drawChart(metrics, container, width, height, categories) {
    for (i = 0; i < metrics.length; ++i) {
        drawMetricGauge(0, i, metrics[i], container, width, height, categories);
    }
}

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
                    setUpGroupSelector(data[i].isGlobal)
                    urlTaiga = data[i].taigaURL;
                    urlGithub = data[i].githubURL;
                }
            }
        }
    });
}

function drawChartByStudent(metrics, container, width, height, categories, projecturls) {
    var gaugeChart = $("#gaugeChart");
    for (j = 0; j < students.length; j++) {
        var divF = document.createElement('div');
        divF.style.marginTop = "1em";
        divF.style.marginBottom = "1em";

        var labelF = document.createElement('label');
        labelF.setAttribute("style", "font-size:20px")
        //labelF.id = students[j].id;
        labelF.textContent = students[j].studentName;
        divF.appendChild(labelF);

        gaugeChart.append(divF);
        for (i = 0; i < students[j].metrics.length; ++i) {
            drawMetricGauge(j, i, students[j].metrics[i], container, width, height, categories);
        }
    }
}

function drawChartByFactor(metrics, container, width, height, categories, projecturls) {
    var gaugeChart = $("#gaugeChart");
    for (j = 0; j < factors.length; j++) {
        var divF = document.createElement('div');
        divF.style.marginTop = "1em";
        divF.style.marginBottom = "1em";
        if (factors[j].type === "Taiga") {
            if (urlTaiga !== undefined) {
                var a = document.createElement('a')
                a.href=urlTaiga;
                var icon = document.createElement("img");
                icon.src = "../icons/taiga_icon.png"
                icon.width = 38;
                icon.height = 25;
                icon.style = "padding-right:15px;";
                a.appendChild(icon)
                divF.appendChild(a);
            }
        }
        if (factors[j].type === "Github") {
            if (urlGithub !== undefined) {
                var list = urlGithub.split(";");
                var a = document.createElement('a')
                a.href=list[0];
                var icon1 = document.createElement("img");
                icon1.src = "../icons/github_icon.png"
                icon1.width = 38;
                icon1.height = 25;
                icon1.style = "padding-right:15px;";
                a.appendChild(icon1)
                divF.appendChild(a);
                if (list.length >= 2) {
                    var a = document.createElement('a')
                    a.href=list[1];
                    var icon2 = document.createElement("img");
                    icon2.src = "../icons/github_icon.png"
                    icon2.width = 38;
                    icon2.height = 25;
                    icon2.style = "padding-right:15px;";
                    a.appendChild(icon2)
                    divF.appendChild(a);
                }
            }
        }


        var labelF = document.createElement('label');
        labelF.id = factors[j].id;
        labelF.textContent = factors[j].name;
        divF.appendChild(labelF);

        var a = document.createElement('a')
        a.classList.add("check")
        a.setAttribute('data-tooltip', factors[j].description)

        var tooltipdiv = document.createElement('div');
        tooltipdiv.classList.add("tooltip");
        var iconF = document.createElement('img');
        iconF.class = "icons";
        iconF.src = "../icons/information.png";
        iconF.width = 38;
        iconF.height = 25;
        iconF.style = "padding-left:15px;";

        var spantootlip = document.createElement('span');
        spantootlip.classList.add("tooltiptext");
        spantootlip.innerHTML = factors[j].description;
        tooltipdiv.appendChild(iconF)
        tooltipdiv.appendChild(spantootlip)
        a.appendChild(iconF)
        divF.appendChild(a);

        gaugeChart.append(divF);
        for (i = 0; i < factors[j].metrics.length; ++i) {
            drawMetricGauge(j, i, factors[j].metrics[i], container, width, height, categories);
        }
    }
    // Add metrics without factor
    var divNOF = document.createElement('div');
    divNOF.id = "divwithoutfactor";
    divNOF.style.marginTop = "1em";
    divNOF.style.marginBottom = "1em";

    var labelNOF = document.createElement('label');
    labelNOF.id = "withoutfactor";
    labelNOF.textContent = "Metrics not associated to any factor";
    divNOF.appendChild(labelNOF);

    metrics.forEach(function (metric) {
        var msvg = document.getElementById(metric.id);
        if (!msvg) {
            if (!document.getElementById("divwithoutfactor"))
                gaugeChart.append(divNOF);
            drawMetricGauge(j, i, metric, container, width, height, categories);
        }
    });
}

function drawMetricGauge(j, i, metric, container, width, height, categories) {
    //0 to 1 values to angular values
    angle = metric.value * 180 + 90;
    upperThresh = 0.66 * Math.PI - Math.PI / 2;
    lowThresh = 0.33 * Math.PI - Math.PI / 2;

    var arc = d3.arc()      //create arc starting at -90 degreees
        .innerRadius(70*width/250)
        .outerRadius(110*width/250)
        .startAngle(-tau);

    //make chart a hyperlink
    var textColor = "#000";
    var findMet = metricsDB.find(function (element) {
        return element.externalId === metric.id;
    });
    if (findMet) { // if metric not found it will be undefined
        urlLink = findMet.webUrl;
    }
    if (urlLink) {
        //create chart svg with hyperlink
        var svg = d3.select(container).append("svg")
            .attr("id", metric.id)
            .attr("width", width)
            .attr("height", height)
            .attr("class", "chart")
            .append("a")
            .attr("xlink:href", function (d) {
                return urlLink
            })
            .attr("target","_blank")
            .append("g")
            .attr("transform",
                "translate(" + width / 2 + "," + height / 2 + ")");
        textColor = "#0177a6";
    } else {
        //create chart svg
        var svg = d3.select(container).append("svg")
            .attr("id", metric.id)
            .attr("width", width)
            .attr("height", height)
            .attr("class", "chart")
            .append("g")
            .attr("transform",
                "translate(" + width / 2 + "," + height / 2 + ")");
    }

    //draw blue background for charts
    svg.append("path")
        .datum({endAngle: Math.PI / 2})
        .style("fill", "#0579A8")
        .attr("d", arc);

    //filtering the appropriate categories
    let metricCategories = categories.filter(function (cat) {
        return cat.name === findMet.categoryName;
    });

    //ordering the result in descendent order
    metricCategories = metricCategories.sort(function (a, b) {
        return b.upperThreshold - a.upperThreshold;
    });

    //if findMet.categoryName is blank or contains a deleted category, the default category is painted
    if (metricCategories.length === 0)
        metricCategories = categories.filter(function (cat) {
            return cat.name === DEFAULT_CATEGORY;
        });

    metricCategories.forEach(function (category) {
        console.log(category);
        var threshold = category.upperThreshold * Math.PI - Math.PI / 2;
        svg.append("path")
            .datum({endAngle: threshold})
            .style("fill", category.color)
            .attr("d", arc);
    });

    //create needle
    var arc2 = d3.arc()
        .innerRadius(0)
        .outerRadius(100*width/250)
        .startAngle(-0.05)
        .endAngle(0.05);

    //draw needle in correct position depending on it's angle
    svg.append("path")
        .style("fill", "#000")
        .attr("d", arc2)
        .attr("transform", "translate(" + -100*width/250 * Math.cos((angle - 90) / 180 * Math.PI) + "," + -100*width/250 * Math.sin((angle - 90) / 180 * Math.PI) + ") rotate(" + angle + ")");

    //create small circle at needle base
    var arc3 = d3.arc()
        .innerRadius(0)
        .outerRadius(10*width/250)
        .startAngle(0)
        .endAngle(Math.PI * 2);

    //draw needle base
    svg.append("path")
        .style("fill", "#000")
        .attr("d", arc3);

    //add text under the gauge
    let name = subdivideMetricName(metric.name, 23);

    for(let cont = 0; cont < name.length; ++cont){
        svg.append("text")
            .attr("id", "name" + i + j + cont)
            .attr("x", 0)
            .attr("y", 50*width/250 + 15*cont)
            .attr("text-anchor", "middle")
            .attr("fill", textColor)
            .attr("title", metric.name)
            .style("font-size", 11+8*width/250+"px")
            .text(name[cont]);
    }

    d3.select("#name"+i+j).append("title").text(metric.name);

    //add label under the text
    var text;
    if (isNaN(metric.value))
        text = metric.value;
    else text = metric.value.toFixed(2);
    svg.append("text")
        .attr("x", 0)
        .attr("y", 50*width/250 + 30 + (name.length - 1) * 10)
        .attr("text-anchor", "middle")
        .attr("fill", textColor)
        .style("font-size", 11+6*width/250+"px")
        .text(text);
}

function sortDataAlphabetically (metrics) {
    function compare (a, b) {
        if (a.name < b.name) return -1;
        else if (a.name > b.name) return 1;
        else return 0;
    }
    metrics.sort(compare);
}
goog.provide('radar')
var radar = (function(){
	function radar(){
		if (window.addEventListener){
			window.addEventListener("load", initDom, false);
		}
		else if (window.attachEvent && !window.opera){
			window.attachEvent("onload", initDom);
		}

		function initDom() {
			identIE();
			if (document.body.id == 'reporting'){
				tabReporting();
			}
		}

		function identIE(){
			goog.dom.classes.add(document.body, ((goog.userAgent.IE && parseInt(goog.userAgent.VERSION) < 9) ? ('ie' + parseInt(goog.userAgent.VERSION)) : ''));
		}


		function tabReporting(parentNode){
			var tabsList = (parentNode || document).querySelector('.tabsContent');
			if(tabsList){
			var firstDiv;
					goog.array.forEach([tabsList],function(tabs){


                				if (!tabs) {
                					return;
                				}

                				var listLi = goog.dom.getElementsByTagNameAndClass('li', null, tabs);

                				goog.dom.classes.add(goog.dom.getElementsByTagNameAndClass('li', null, tabs)[0], 'current');

                				goog.array.forEach(goog.dom.getElementsByTagNameAndClass('li', null, tabs), function(li){
                					var link = goog.dom.getElementsByTagNameAndClass('a', null, li)[0];
                					var text = link.href.split('#')[1];
                					var trError = goog.dom.getElementsByTagNameAndClass('tr', 'error', goog.dom.getElement(text))[0];

                					if(trError){
                						var textError = goog.dom.createDom('span', {'class':'error'}, '');
                						goog.dom.appendChild(li, textError);
                					}
                				});
                //				Get current holder-tabs
                				 var currentHolderTabs = goog.dom.getElementsByClass('holder-tabs',tabs.parentNode)[0]

                				goog.array.forEach(goog.dom.getChildren(currentHolderTabs), function(en){
                					goog.style.showElement(en , false);
                				});

                				firstDiv = goog.dom.getElementsByTagNameAndClass('div', null, goog.dom.getElementsByClass('holder-tabs',tabs.parentNode)[0])[0];
                				goog.style.showElement(firstDiv, true);


                				goog.array.forEach(goog.dom.getElementsByTagNameAndClass('li', 'tabs-control-item'), function(ev){
                					goog.events.listen(ev, 'click',selectActiveTab);

                					 function selectActiveTab(e , element){
                					 	if(element){
                					 		for(var i = 0 ; i < element.children.length; i++) {
												if(goog.dom.classes.has(element.children[i], 'current')){
													return
												}
                					 		}

                					 	}
										var tabs = goog.dom.getAncestorByTagNameAndClass(e.target,'ul','tabsContent')


//										Current title tab

										var titleTab = goog.dom.getElementsByTagNameAndClass('li', null, element || tabs)
										goog.array.forEach(titleTab, function(ec){
											goog.dom.classes.remove(ec, 'current');
										})
										goog.dom.classes.add(element ? titleTab[0] :this, 'current');


										var link = goog.dom.getElementsByTagNameAndClass('a', null, element ? titleTab[0] :this)[0];
										var text = link.href.split('#')[1];
//										Current content tab
										var holderTabs = goog.dom.getElementByClass('holder-tabs',(element || tabs).parentNode);
										var contentTab = goog.dom.getChildren(holderTabs)
										goog.array.forEach(contentTab, function(en){
											goog.style.showElement(en , false);
											goog.dom.classes.remove(en, 'current');
										});
										var currentWrapTab = goog.dom.getElementByClass(text, holderTabs) || document.getElementById(text);

										goog.style.showElement(currentWrapTab, true);
										goog.dom.classes.add(currentWrapTab, 'current') ;

										var underTab = goog.dom.getElementByClass('tabsContent',currentWrapTab);
										if(underTab){
											selectActiveTab(e,underTab)
										}

										e.preventDefault();
									}

                				});

                			})

					tabReporting(firstDiv)
			}


		};
}
	return radar
}())

goog.provide('tabReporting')
var currentData = [];
var tabReporting=(function(){
	function tabReporting(){
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
				window.onunload = function() {
					sessionStorage.tabActive = JSON.stringify(currentData);
                };
			}
		}

		function identIE(){
			goog.dom.classes.add(document.body, ((goog.userAgent.IE && parseInt(goog.userAgent.VERSION) < 9) ? ('ie' + parseInt(goog.userAgent.VERSION)) : ''));
		}

		function tabReporting(){
			var width = goog.style.getSize(goog.dom.getElement('reporting')).width;
			var tabsList = goog.dom.getElementsByClass('tabsContent');


			goog.array.forEach(tabsList,function(tabs){
				if (!tabs) {
						return;
					}
					if(width > 750){

						var listLi = goog.dom.getElementsByTagNameAndClass('li', null, tabs);
						var listActiveTab = sessionStorage.tabActive
						var defaultValue = true;
						if(listActiveTab){
							var getdata = currentData = JSON.parse(sessionStorage.tabActive);
						}

						if(getdata && getdata.length){
							goog.array.forEach(getdata,function(elem){
								var item = elem.href;
								if(tabs.parentNode.className == elem.parent){
									goog.array.forEach(goog.dom.getElementsByTagNameAndClass('a', null, tabs.parentNode), function(a){
										var tab = a.href.split('#')[1];
										if (tab == item){
											goog.array.forEach(goog.dom.getElementsByTagNameAndClass('div', null, goog.dom.getElementsByClass('holder-tabs',tabs.parentNode)[0]), function(en){
												goog.style.showElement(en , false);
											});

											goog.style.showElement(goog.dom.getElement(tab), true);
											goog.dom.classes.set(goog.dom.getElement(tab), 'current');

											var li =  goog.dom.getPreviousNode(a);
											goog.dom.classes.set(li, 'current');
										}
									});
									defaultValue = false
								}
							});
							if(defaultValue){
								defaultActive(tabs)
							}

						}
						else{
							defaultActive(tabs)
						}

						goog.array.forEach(goog.dom.getElementsByTagNameAndClass('li', null, tabs), function(li){
							var link = goog.dom.getElementsByTagNameAndClass('a', null, li)[0];
							var text = link.href.split('#')[1];
							var trError = goog.dom.getElementsByTagNameAndClass('tr', 'error', goog.dom.getElement(text))[0];

							if(trError){
								var textError = goog.dom.createDom('span', {'class':'error'}, '');
								goog.dom.appendChild(li, textError);
							}
						});

						goog.array.forEach(goog.dom.getElementsByTagNameAndClass('li', null, tabs), function(ev){
							goog.events.listen(ev, 'click', function(el){
								if(!goog.dom.classes.has(this, 'current')){

									goog.array.forEach(goog.dom.getElementsByTagNameAndClass('li', null, tabs), function(ec){
										goog.dom.classes.remove(ec, 'current');
									})
									goog.dom.classes.set(this, 'current');
									var parent = goog.dom.getAncestorByClass(el, null);

								}

								var link = goog.dom.getElementsByTagNameAndClass('a', null, this)[0];
								var text = link.href.split('#')[1];

								goog.array.forEach(goog.dom.getElementsByTagNameAndClass('div', null, goog.dom.getElementsByClass('holder-tabs',tabs.parentNode)[0]), function(en){
									goog.style.showElement(en , false);
									goog.dom.classes.remove(en, 'current');
								});

								goog.style.showElement(goog.dom.getElement(text), true);
								goog.dom.classes.set(goog.dom.getElement(text), 'current') ;

								addToStorage(goog.dom.getAncestorByTagNameAndClass(this, 'div').className,link.href.split('#')[1])
								el.preventDefault();
							});

						});
					}
					else if(width < 750 && isMobile){
						goog.array.forEach(goog.dom.getElementsByTagNameAndClass('li', null, tabs), function(li){
							var link = goog.dom.getElementsByTagNameAndClass('a', null, li)[0];
							var text = link.href.split('#')[1];
							var trError = goog.dom.getElementsByTagNameAndClass('tr', 'error', goog.dom.getElement(text))[0];

							if(trError){
								goog.dom.classes.set(li, 'iconError');
							}
							goog.array.forEach(goog.dom.getElementsByTagNameAndClass('div', null, goog.dom.getElementsByClass('holder-tabs',tabs.parentNode)[0]), function(div){
								goog.style.setStyle(div, "display", "none");
								if(text == div.id){
									var newDiv = div.cloneNode(true);
									goog.dom.appendChild(li, newDiv)
								}
							})
							goog.events.listen(li, 'click', function(el){
								if(!goog.dom.classes.has(this, 'open')){
									goog.dom.classes.add(this, 'open');
									goog.style.setStyle(goog.dom.getElementsByTagNameAndClass('div', null, this)[0], "display", "block");
								}
								else{
									goog.dom.classes.remove(this, 'open');
									goog.style.setStyle(goog.dom.getElementsByTagNameAndClass('div', null, this)[0], "display", "none");
								}
								el.preventDefault();
							});
						});
					}
			})

		};

		function addToStorage(parent,elem){
			if(parent && elem){
				var obj = {};
				var unicValue = true;
				if(currentData.length){
					for(var i=0;i<currentData.length;i++){
						if(currentData[i].parent==parent){
							unicValue = false;
							if(currentData[i].href != elem){
								currentData.splice(i--	,1)
								obj.parent = parent;
								obj.href = elem;
								currentData.push(obj);
							}

						}
					}
					if(unicValue){
						obj.parent = parent;
						obj.href = elem;

						currentData.push(obj);

					}
				}else{
					obj.parent = parent;
					obj.href = elem;
					currentData.push(obj);
				}


			}

		}
		function defaultActive(tabs){
			goog.array.forEach(goog.dom.getElementsByTagNameAndClass('div', null,  goog.dom.getElementsByClass('holder-tabs',tabs.parentNode)[0]), function(en){
				goog.style.showElement(en , false);
			});

			var firstDiv = goog.dom.getElementsByTagNameAndClass('div', null, goog.dom.getElementsByClass('holder-tabs',tabs.parentNode)[0])[0];
			goog.style.showElement(firstDiv, true);

			goog.dom.classes.add(goog.dom.getElementsByTagNameAndClass('li', null, tabs)[0], 'current');
		}
	};
	return tabReporting
})()
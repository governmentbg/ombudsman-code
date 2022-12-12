 <div class="col">
     <h2 class="m-home-title mt-4 mr-5"> {{ trans('common.trending') }}</h2>
     <div class="m-position">
         <ul class="list">
             @php
                 
                 $data = \App\Http\Controllers\MNewsController::frontNews(App::getLocale(), 10, 0, 2, 0);
             @endphp
             {{-- {{ dd($data) }} --}}
             @foreach ($data as $act)
                 {{-- {{ dd($act['MnL_title']) }} --}}
                 <li>
                     <a href="/{{ App::getLocale() }}/n/{{ $act['MnL_path'] }}">
                         {{ $act['MnL_title'] }}
                     </a>
                     <div class="m-d"></div>

                 </li>
             @endforeach
         </ul>
     </div>
     <div class="link-m mt-5"><a
             href="/{{ App::getLocale() }}/p/{{ getPathById(App::getLocale(), 52) }}">{{ trans('common.read_more') }}
             <i class="cil-arrow-right"></i> </a></div>

 </div>

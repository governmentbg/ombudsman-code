@extends('layouts.default')
@section('title', $data->ArL_title . ' - ' . trans('common.title'))
@section('site-description', $data->Ar_name)
@section('site-keywords', trans('page-keywords'))
{{-- @section('site-canonical', $canonical) --}}
{{-- @section('page-hreflang', $canonical) --}}
@section('article-headline', $data->ArL_title)
{{-- {{-- @section('article-date', $publishDate) --}}
@section('article-modification-date', $data->created_at)
@section('article-description', trans('page-description'))
@section('article-tag', '')


@section('content')
    @include('layouts.breadcrumb')

    <div class="row">
        <div class="col ">
            <h1 class="t-h1">{{ $data->ArL_title }}</h1>
        </div>
    </div>

    <div class="m-article">

        @foreach ($res as $ev)
            {{-- {{dd($ev->Mv_date)}} --}}
            <div class="row ev-list" id="e{{ $ev->MvL_id }}">
                <div class="col-sm-12">
                    <div class="ev-head">
                        <div class="title">
                            {{ $ev->MvL_title }}
                        </div>


                        {{ df(App::getLocale(), $ev->Mv_date, 1) }}
                    </div>
                    <div class="ev-body">
                        {!! $ev->MvL_body !!}
                    </div>

                    <div class="mt-5">
                        <ul class="list-unstyled m-list">
                            {{-- {{ dd($data->files) }} --}}
                            @foreach ($ev->files as $n)
                                <li class="mb-2">

                                    <span class="m-badge-1">
                                        <i class="cil-{{ $n->ArF_type }}"></i>
                                    </span>
                                    <a href="/storage{{ $n->ArF_file }}">

                                        {{ $n->ArF_name }}
                                    </a>
                                    @if ($n->ArF_size)
                                        <span class="m-intro-date">
                                            ({{ number_format($n->ArF_size / 1024, 2) }} kb)
                                        </span>
                                    @endif
                                    {{-- ArF_size --}}
                                    {{-- ArF_desc --}}
                                    {{-- ArF_date --}}
                                    @if ($n->ArF_date)
                                        <span class="m-intro-date">
                                            {{ df(App::getLocale(), $n->ArF_date, 0) }}

                                        </span>
                                    @endif
                                    @if ($n->ArF_desc)
                                        <div class="m-intro-accent">
                                            {{ $n->ArF_desc }}
                                        </div>
                                    @endif


                                </li>
                            @endforeach
                        </ul>

                    </div>

                    @if ($ev->Str_url)
                        <video id="my-video" class="video-js mb-3" controls preload="auto" width="640" height="264"
                            {{-- poster="MY_VIDEO_POSTER.jpg" --}} data-setup="{}">
                            <source src="{{ $ev->Str_url }}" type="video/mp4" />
                            {{-- <source src="MY_VIDEO.webm" type="video/webm" /> --}}
                            <p class="vjs-no-js">
                                To view this video please enable JavaScript, and consider upgrading to a
                                web browser that
                                <a href="https://videojs.com/html5-video-support/" target="_blank">supports HTML5 video</a>
                            </p>
                        </video>
                    @endif
                    @if ($ev->Str_embed)
                        @if (substr($ev->Str_embed, 0, 4) == 'http')
                            <div class="row">
                                <div class="col-sm-12 col-xs-12 col-md-10 col-lg-8 col-xl-6">
                                    <x-embed url="{{ $ev->Str_embed }}" class="mb-3" />
                                </div>
                            </div>
                        @else
                            {!! $ev->Str_embed !!}
                        @endif
                    @endif
                </div>
            </div>





            {{-- {{ddd($ev["media"])}} --}}
        @endforeach
    </div>

    {{-- <x-embed
        url="https://www.facebook.com/5min.crafts/videos/renovation-has-never-been-so-easy-just-use-these-smart-repair-tricks-/557552639126740/" /> --}}


    {{-- </div>
    </div> --}}





@stop

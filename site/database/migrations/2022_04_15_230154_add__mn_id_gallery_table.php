<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class AddMnIdGalleryTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::table('m_gallery', function (Blueprint $table) {
            $table->integer('Mn_id')->comment('News ID')->after('Ar_id')->nullable()->unsigned();


            $table->foreign('Mn_id')->references('Mn_id')->on('m_news');
        });
    }

    /**
     * Reverse the migrations.
     *
     * @return void
     */
    public function down()
    {
        Schema::table('m_gallery', function (Blueprint $table) {
            //
        });
    }
}

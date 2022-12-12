<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class AddPinToMnewsTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::table('m_news', function (Blueprint $table) {
            $table->boolean('Mn_pin')->comment('Pinned')->after('Mn_date')->nullable()->index();
        });
    }

    /** 
     * Reverse the migrations.
     *
     * @return void
     */
    public function down()
    {
        Schema::table('m_news', function (Blueprint $table) {
            //
        });
    }
}
